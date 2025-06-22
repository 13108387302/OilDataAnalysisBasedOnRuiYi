import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from torch.utils.data import DataLoader, TensorDataset
from sklearn.preprocessing import MinMaxScaler
from sklearn.model_selection import train_test_split
from app.algorithms.base_algorithm import BaseAlgorithm
from pathlib import Path

# 1. PyTorch TCN Model Definition
class TCN(nn.Module):
    def __init__(self, input_size, output_size, num_channels, kernel_size, dropout):
        super(TCN, self).__init__()
        self.tcn = nn.Sequential(
            nn.Conv1d(input_size, num_channels, kernel_size=kernel_size, padding=(kernel_size - 1)),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Conv1d(num_channels, num_channels, kernel_size=kernel_size, padding=(kernel_size - 1)),
            nn.ReLU(),
            nn.Dropout(dropout)
        )
        self.linear = nn.Linear(num_channels, output_size)

    def forward(self, x):
        # PyTorch Conv1d expects (N, C, L), so we permute dimensions
        x = x.permute(0, 2, 1)
        y = self.tcn(x)
        # Get the last output of the sequence
        y = y[:, :, -1]
        return self.linear(y)

# 2. PyTorch Trainer Implementation
class Trainer(BaseAlgorithm):
    def __init__(self, file_path: str, output_dir: str, params: dict):
        super().__init__(file_path, output_dir, params)
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        
        # Algorithm-specific parameters
        self.sequence_length = int(self.params.get('sequence_length', 60))
        self.epochs = int(self.params.get('epochs', 100))
        self.batch_size = int(self.params.get('batch_size', 32))
        self.learning_rate = float(self.params.get('learning_rate', 0.001))
        self.num_channels = int(self.params.get('num_channels', 64))
        self.kernel_size = int(self.params.get('kernel_size', 3))
        self.dropout = float(self.params.get('dropout', 0.2))

    def _create_sequences(self, data, target):
        X, y = [], []
        for i in range(len(data) - self.sequence_length):
            X.append(data[i:(i + self.sequence_length)])
            y.append(target[i + self.sequence_length])
        return np.array(X), np.array(y)

    def _compute(self, data: pd.DataFrame) -> dict:
        feature_columns = self.params['feature_columns']
        target_column = self.params['target_column']
        
        # Data scaling
        features = data[feature_columns].values
        target = data[target_column].values.reshape(-1, 1)
        self.feature_scaler = MinMaxScaler()
        features_scaled = self.feature_scaler.fit_transform(features)
        self.target_scaler = MinMaxScaler()
        target_scaled = self.target_scaler.fit_transform(target)

        # Create sequences
        X_seq, y_seq = self._create_sequences(features_scaled, target_scaled)
        if X_seq.shape[0] == 0:
            raise ValueError("Not enough data for sequences. Please provide more data or reduce sequence_length.")

        # For regression, we typically split data sequentially to respect time order,
        # but for this example, we'll stick to random split for now.
        # A proper time-series split would be:
        # train_size = int(len(X_seq) * 0.8)
        # X_train, X_test = X_seq[:train_size], X_seq[train_size:]
        # y_train, y_test = y_seq[:train_size], y_seq[train_size:]
        
        X_train, X_test, y_train, y_test = train_test_split(
            X_seq, y_seq, test_size=0.2, random_state=42
        )

        # Convert to PyTorch tensors and create DataLoaders
        X_train_tensor = torch.FloatTensor(X_train).to(self.device)
        y_train_tensor = torch.FloatTensor(y_train).to(self.device)
        X_test_tensor = torch.FloatTensor(X_test).to(self.device)
        
        train_dataset = TensorDataset(X_train_tensor, y_train_tensor)
        train_loader = DataLoader(dataset=train_dataset, batch_size=self.batch_size, shuffle=True)

        # Model, Loss, Optimizer
        model = TCN(
            input_size=X_train.shape[2],
            output_size=1,
            num_channels=self.num_channels,
            kernel_size=self.kernel_size,
            dropout=self.dropout
        ).to(self.device)
        criterion = nn.MSELoss()
        optimizer = torch.optim.Adam(model.parameters(), lr=self.learning_rate)

        # Training loop
        model.train()
        for epoch in range(self.epochs):
            for i, (inputs, labels) in enumerate(train_loader):
                optimizer.zero_grad()
                outputs = model(inputs)
                loss = criterion(outputs, labels)
                loss.backward()
                optimizer.step()
            
            if (epoch + 1) % 10 == 0:
                print(f'TCN Training - Epoch [{epoch+1}/{self.epochs}], Loss: {loss.item():.4f}')

        # Evaluation
        model.eval()
        with torch.no_grad():
            y_pred_tensor = model(X_test_tensor)
        
        y_pred_scaled = y_pred_tensor.cpu().numpy()
        y_pred = self.target_scaler.inverse_transform(y_pred_scaled)
        y_true = self.target_scaler.inverse_transform(y_test)

        from app.services.results_utils import calculate_regression_metrics
        metrics = calculate_regression_metrics(y_true, y_pred)
        
        return {
            "statistics": metrics,
            "model_params": {
                "sequence_length": int(self.sequence_length),
                "num_channels": self.num_channels,
                "kernel_size": int(self.kernel_size),
                "epochs": int(self.epochs),
                "batch_size": int(self.batch_size),
                "learning_rate": float(self.learning_rate),
                "dropout": float(self.dropout),
                "feature_columns": feature_columns,
                "target_column": target_column
            },
            "model": model,
            # 标准数据字段
            "predictions": y_pred.flatten(),
            "actual_values": y_true.flatten(),
            "feature_values": X_test.reshape(-1, X_test.shape[-1]),
            "input_sample": data[feature_columns + [target_column]].head(100).to_dict('records')
        }

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: "Path") -> dict:
        from app.services.results_utils import plot_regression_results
        import numpy as np

        # 安全地获取数据，避免numpy数组的or操作歧义
        y_true = computed_data.get('actual_values')
        if y_true is None:
            y_true = computed_data.get('y_true')

        y_pred = computed_data.get('predictions')
        if y_pred is None:
            y_pred = computed_data.get('y_pred')

        if y_true is None or y_pred is None:
            return {}

        # 确保数据是numpy数组格式，因为可能已经被序列化为列表
        if isinstance(y_true, list):
            y_true = np.array(y_true)
        if isinstance(y_pred, list):
            y_pred = np.array(y_pred)

        # 如果是numpy数组，则flatten；如果已经是1D，则直接使用
        if hasattr(y_true, 'flatten'):
            y_true = y_true.flatten()
        if hasattr(y_pred, 'flatten'):
            y_pred = y_pred.flatten()

        visualization_path = plot_regression_results(
            output_dir, y_true, y_pred, "TCN Regression"
        )

        return {
            "regression_results_plot": str(visualization_path)
        }
    
    def _save_model(self, computed_data: dict, output_dir: "Path") -> dict:
        model = computed_data.get("model")
        if not model:
            return {}

        model_path = output_dir / "tcn_model.pth"
        try:
            torch.save(model.state_dict(), model_path)
            print(f"Model saved to {model_path}")
            # Return a relative path for the API response
            return {"tcn_model_path": str(model_path.relative_to(Path.cwd()))}
        except Exception as e:
            print(f"Error saving model: {e}")
            return {}