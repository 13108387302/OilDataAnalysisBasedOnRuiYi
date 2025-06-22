# app/algorithms/regression/linear.py
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import r2_score, mean_squared_error, mean_absolute_error
import numpy as np
from pathlib import Path
from joblib import dump, load

from app.algorithms.base_algorithm import BaseAlgorithm
from app.visualizations.plot_factory import plot_regression
from app.algorithms.base_predictor import BasePredictorAlgorithm

class Trainer(BaseAlgorithm):
    """
    Performs linear regression on given data.
    """
    def validate_params(self):
        """
        Validates parameters for linear regression.
        Requires 'feature_columns' (list with one element) and 'target_column'.
        """
        # Call parent validation first (handles string to list conversion)
        super().validate_params()

        # Linear regression specific validation: must have exactly one feature column
        if len(self.params['feature_columns']) != 1:
            raise ValueError("Linear regression requires exactly one feature column.")

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Trains a linear regression model and returns evaluation metrics.
        """
        feature_col = self.params['feature_columns'][0]
        target_col = self.params['target_column']
        test_size = self.params.get('test_size', 0.2)
        random_state = self.params.get('random_state', 42)

        # Prepare data
        X = dataframe[[feature_col]].values
        y = dataframe[target_col].values
        
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=test_size, random_state=random_state
        )

        # Train model
        model = LinearRegression()
        model.fit(X_train, y_train)

        # Make predictions
        y_pred = model.predict(X_test)

        # Calculate metrics
        r2 = r2_score(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)
        mae = mean_absolute_error(y_test, y_pred)
        
        # Also predict on the full dataset for visualization
        full_pred = model.predict(X)

        return {
            "statistics": {
                "r2_score": float(r2),
                "mean_squared_error": float(mse),
                "mean_absolute_error": float(mae),
                "rmse": float(mse ** 0.5)
            },
            "model_params": {
                "coefficient": model.coef_,
                "intercept": float(model.intercept_),
                "feature_columns": self.params['feature_columns'],
                "target_column": target_col
            },
            "model_object": model,
            # 标准数据字段
            "predictions": y_pred,
            "actual_values": y_test,
            "feature_values": X_test,
            "input_sample": dataframe[self.params['feature_columns'] + [target_col]].head(100).to_dict('records')
        }

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Generates a regression plot.
        """
        # 从model_params中获取参数
        model_params = computed_data.get('model_params', {})
        feature_cols = model_params.get('feature_columns')
        target_col = model_params.get('target_column')

        if not feature_cols or not target_col:
            return {}

        feature_col = feature_cols[0]

        # 获取测试集的特征值和预测值（长度应该匹配）
        feature_values = computed_data.get('feature_values')
        predictions = computed_data.get('predictions')

        if feature_values is None or predictions is None:
            return {}

        # 确保使用测试集的特征值，而不是完整数据集
        if len(feature_values) > 0:
            # feature_values是二维数组，取第一列作为x值
            x_values = [row[0] for row in feature_values]
        else:
            return {}

        # Sort values for a clean line plot
        plot_df = pd.DataFrame({
            'x': x_values,
            'y_pred': predictions
        }).sort_values(by='x')

        plot_filename = plot_regression(
            dataframe=dataframe,
            x_col=feature_col,
            y_col=target_col,
            y_pred=plot_df['y_pred'],
            x_pred=plot_df['x'],  # 传递排序后的x值
            output_dir=output_dir,
            title=f"Linear Regression Fit: {target_col} vs {feature_col}",
            file_name=f"linear_regression_{target_col}_vs_{feature_col}.png"
        )

        return {
            "regression_plot": plot_filename
        }

    def _save_to_excel(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the test set data along with the model's predictions to an Excel file.
        """
        # 从model_params中获取参数
        model_params = computed_data.get('model_params', {})
        feature_cols = model_params.get('feature_columns')
        target_col = model_params.get('target_column')

        if not feature_cols or not target_col:
            return {}

        # 使用标准字段名
        feature_values = computed_data.get('feature_values')
        actual_values = computed_data.get('actual_values')
        predictions = computed_data.get('predictions')

        if feature_values is None or actual_values is None or predictions is None:
            return {}

        x_test_df = pd.DataFrame(feature_values, columns=feature_cols)

        report_df = pd.DataFrame({
            f'actual_{target_col}': actual_values,
            'predicted_y': predictions
        })

        # Concatenate features and results
        full_report_df = pd.concat([x_test_df, report_df], axis=1)

        # Define the output file path
        file_name = f"linear_regression_report_{target_col}.xlsx"
        output_path = output_dir / file_name

        # Save to Excel
        full_report_df.to_excel(output_path, index=False)

        return {
            "report_file": file_name
        }

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the trained Linear Regression model to a file.
        """
        model = computed_data.get('model_object')
        if model is None:
            return {}
            
        file_name = "linear_regression_model.joblib"
        output_path = output_dir / file_name
        
        dump(model, output_path)
        
        return { "model_file": file_name }

class Predictor(BasePredictorAlgorithm):
    """
    Predicts using a pre-trained Linear Regression model.
    """
    def _load_model(self, model_path: Path):
        """Loads the saved Linear Regression model."""
        return load(model_path)

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        """Runs prediction on new data."""
        feature_col = self.params['feature_columns'][0]
        X_new = dataframe[[feature_col]].values
        
        predictions = model_artifact.predict(X_new)
        return pd.Series(predictions, name='predicted_y') 