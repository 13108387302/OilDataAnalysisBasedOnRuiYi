# app/algorithms/classification/knn.py
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix
from sklearn.preprocessing import StandardScaler, LabelEncoder
from pathlib import Path
from joblib import dump, load
import numpy as np

from app.algorithms.base_algorithm import BaseAlgorithm
from app.visualizations.plot_factory import plot_confusion_matrix, plot_roc_curve
from app.algorithms.base_predictor import BasePredictorAlgorithm

class Trainer(BaseAlgorithm):
    """
    Performs K-Nearest Neighbors (KNN) for classification.
    """
    def validate_params(self):
        """
        Validates parameters for KNN Classification.
        """
        if 'feature_columns' not in self.params:
            raise ValueError("Parameter 'feature_columns' is required.")
        if 'target_column' not in self.params:
            raise ValueError("Parameter 'target_column' is required.")
        if 'n_neighbors' not in self.params:
            raise ValueError("Parameter 'n_neighbors' (K value) is required.")

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Trains a KNN classifier and returns evaluation metrics.
        """
        feature_cols = self.params['feature_columns']
        if isinstance(feature_cols, str):
            feature_cols = [feature_cols]

        target_col = self.params['target_column']
        n_neighbors = int(self.params['n_neighbors'])
        test_size = self.params.get('test_size', 0.2)
        random_state = self.params.get('random_state', 42)
        
        # Prepare data
        X = dataframe[feature_cols]
        y = dataframe[target_col]

        # Encode target variable to numeric labels
        le = LabelEncoder()
        y_encoded = le.fit_transform(y)
        class_names = le.classes_.tolist()

        # 检查每个类别的样本数量
        _, class_counts = np.unique(y_encoded, return_counts=True)
        min_class_count = min(class_counts)

        # 如果最小类别样本数少于2，无法进行分层抽样
        if min_class_count < 2:
            print(f"警告: 最小类别样本数为 {min_class_count}，无法进行分层抽样，将使用随机抽样")
            stratify_param = None
        else:
            stratify_param = y_encoded

        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            X, y_encoded, test_size=test_size, random_state=random_state, stratify=stratify_param
        )

        # Scale features
        scaler = StandardScaler()
        X_train_scaled = scaler.fit_transform(X_train)
        X_test_scaled = scaler.transform(X_test)

        # Train model
        model = KNeighborsClassifier(n_neighbors=n_neighbors, n_jobs=-1)
        model.fit(X_train_scaled, y_train)

        # Make predictions
        y_pred = model.predict(X_test_scaled)
        y_prob = model.predict_proba(X_test_scaled)
        
        # Calculate metrics
        accuracy = accuracy_score(y_test, y_pred)
        precision = precision_score(y_test, y_pred, average='weighted', zero_division=0)
        recall = recall_score(y_test, y_pred, average='weighted', zero_division=0)
        f1 = f1_score(y_test, y_pred, average='weighted', zero_division=0)
        conf_matrix = confusion_matrix(y_test, y_pred).tolist()

        return {
            "statistics": {
                "accuracy": float(accuracy),
                "precision": float(precision),
                "recall": float(recall),
                "f1_score": float(f1),
                "confusion_matrix": conf_matrix,
            },
            "model_params": {
                "n_neighbors": int(n_neighbors),
                "feature_columns": feature_cols,
                "target_column": target_col
            },
            "model_object": model,
            "scaler": scaler,
            "label_encoder": le,
            "class_names": class_names,
            # 标准数据字段
            "predictions": y_pred,
            "actual_values": y_test,
            "probabilities": y_prob,
            "feature_values": X_test_scaled,
            "input_sample": dataframe[feature_cols + [target_col]].head(100).to_dict('records')
        }

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the trained KNN model, scaler, and label encoder.
        """
        model_package = {
            'model': computed_data.get('model_object'),
            'scaler': computed_data.get('scaler'),
            'label_encoder': computed_data.get('label_encoder'),
            'feature_columns': computed_data.get('feature_columns')
        }
        
        file_name = "knn_classification_model.joblib"
        output_path = output_dir / file_name
        dump(model_package, output_path)
        
        return { "model_file": file_name }

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Generates classification plots.
        """
        visualizations = {}

        # 安全地获取数据，避免numpy数组的or操作歧义
        y_test = computed_data.get('actual_values')
        if y_test is None:
            y_test = computed_data.get('y_test')

        y_pred = computed_data.get('predictions')
        if y_pred is None:
            y_pred = computed_data.get('y_pred')

        class_names = computed_data.get('class_names')
        
        cm_filename = plot_confusion_matrix(
            y_true=y_test, y_pred=y_pred, class_names=class_names,
            output_dir=output_dir, file_name="knn_confusion_matrix.png", title="KNN Confusion Matrix"
        )
        visualizations["confusion_matrix_plot"] = cm_filename

        y_prob = computed_data.get('probabilities')
        if y_prob is None:
            y_prob = computed_data.get('y_prob')
        # ROC curve is typically for binary classification, but can be extended.
        # Plotting one-vs-rest for each class.
        if y_prob is not None and len(class_names) > 1:
            roc_filename = plot_roc_curve(
                y_true=y_test, y_prob=y_prob, class_names=class_names,
                output_dir=output_dir, file_name="knn_roc_curve.png", title="KNN ROC Curve (One-vs-Rest)"
            )
            visualizations["roc_curve_plot"] = roc_filename
            
        return visualizations
    
    def _save_to_excel(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        # 使用标准字段名，安全地获取数据
        y_test = computed_data.get('actual_values')
        if y_test is None:
            y_test = computed_data.get('y_test')

        y_pred = computed_data.get('predictions')
        if y_pred is None:
            y_pred = computed_data.get('y_pred')

        label_encoder = computed_data.get('label_encoder')

        if y_test is None or y_pred is None or label_encoder is None:
            return {}

        report_df = pd.DataFrame({
            'y_true': label_encoder.inverse_transform(y_test),
            'y_predicted': label_encoder.inverse_transform(y_pred)
        })
        file_name = f"knn_classification_report.xlsx"
        output_path = output_dir / file_name
        report_df.to_excel(output_path, index=False)
        return {"report_file": file_name}


class Predictor(BasePredictorAlgorithm):
    """
    Predicts using a pre-trained KNN model.
    """
    def _load_model(self, model_path: Path):
        """Loads the saved KNN model package."""
        return load(model_path)

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        """Runs prediction on new data."""
        model = model_artifact['model']
        scaler = model_artifact['scaler']
        le = model_artifact['label_encoder']
        feature_cols = model_artifact['feature_columns']

        missing_cols = set(feature_cols) - set(dataframe.columns)
        if missing_cols:
            raise ValueError(f"Missing required columns for prediction: {list(missing_cols)}")

        X_new = dataframe[feature_cols]
        X_scaled = scaler.transform(X_new)
        
        predictions_encoded = model.predict(X_scaled)
        predictions_decoded = le.inverse_transform(predictions_encoded)
        
        return pd.Series(predictions_decoded, name='predicted_class') 
