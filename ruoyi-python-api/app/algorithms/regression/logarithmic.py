import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.metrics import r2_score, mean_squared_error, mean_absolute_error
from pathlib import Path
from joblib import dump, load

from app.algorithms.base_algorithm import BaseAlgorithm
from app.visualizations.plot_factory import plot_regression
from app.algorithms.base_predictor import BasePredictorAlgorithm


class Trainer(BaseAlgorithm):
    """
    Performs Logarithmic Regression by transforming the independent variable.
    Fits a model of the form: y = a + b * log(x)
    """
    def validate_params(self):
        """
        Validates parameters for Logarithmic Regression.
        """
        # Call parent validation first (handles string to list conversion)
        super().validate_params()

        # Logarithmic regression specific validation: must have exactly one feature column
        if len(self.params['feature_columns']) != 1:
            raise ValueError("Logarithmic regression requires exactly one feature column.")

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Fits a logarithmic model and returns evaluation metrics.
        """
        feature_col = self.params['feature_columns'][0]
        target_col = self.params['target_column']

        # Drop rows with NaN and non-positive x values (as log is undefined)
        clean_df = dataframe[[feature_col, target_col]].dropna()
        clean_df = clean_df[clean_df[feature_col] > 0].sort_values(by=feature_col)

        X_orig = clean_df[feature_col].values
        y = clean_df[target_col].values

        if len(X_orig) < 2:
            raise ValueError("Not enough data points with positive X values to perform logarithmic regression.")

        # Transform the independent variable
        X_log = np.log(X_orig).reshape(-1, 1)
        
        # Fit a linear model to the transformed data
        model = LinearRegression()
        model.fit(X_log, y)
        
        # Get parameters: y = intercept + coef * log(x)
        b = model.coef_[0]
        a = model.intercept_
        
        # Make predictions on the original domain
        y_pred = model.predict(X_log)

        # Calculate metrics
        r2 = r2_score(y, y_pred)
        
        # For visualization, we predict on the full original dataframe's X values
        full_x_df = dataframe[[feature_col]].dropna()
        full_x_df = full_x_df[full_x_df[feature_col] > 0].sort_values(by=feature_col)
        full_x_orig = full_x_df[feature_col].values
        full_x_log = np.log(full_x_orig).reshape(-1, 1)
        full_pred = model.predict(full_x_log)

        # 确保数据格式一致性 - 使用与其他算法相同的数据结构
        # 获取对应的实际值（与预测值长度匹配）
        full_y_actual = np.interp(full_x_orig, X_orig, y)

        return {
            "statistics": {
                "r2_score": float(r2),
                "equation": f"y = {a:.4f} + {b:.4f} * log(x)",
                "mean_squared_error": float(mean_squared_error(y, y_pred)),
                "mean_absolute_error": float(mean_absolute_error(y, y_pred)),
                "rmse": float(mean_squared_error(y, y_pred) ** 0.5)
            },
            "model_params": {
                "a": float(a),
                "b": float(b),
                "intercept": float(a),
                "coefficient": float(b),
                "feature_columns": self.params['feature_columns'],
                "target_column": target_col
            },
            "model_object": model,
            # 标准数据字段 - 确保与其他算法格式一致
            "predictions": self.convert_to_serializable(full_pred.tolist() if hasattr(full_pred, 'tolist') else full_pred),
            "actual_values": self.convert_to_serializable(full_y_actual.tolist() if hasattr(full_y_actual, 'tolist') else full_y_actual),
            "feature_values": [[float(x)] for x in (full_x_orig.tolist() if hasattr(full_x_orig, 'tolist') else full_x_orig)],  # 转换为二维数组格式
            "sorted_x": self.convert_to_serializable(full_x_orig),
            "sorted_y_pred": self.convert_to_serializable(full_pred),
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
        sorted_x = computed_data.get('sorted_x')
        sorted_y_pred = computed_data.get('sorted_y_pred')

        if sorted_x is None or sorted_y_pred is None:
            return {}

        plot_df = pd.DataFrame({
            feature_col: sorted_x,
            'y_pred': sorted_y_pred
        })
        
        plot_filename = plot_regression(
            dataframe=dataframe,
            x_col=feature_col,
            y_col=target_col,
            y_pred=plot_df['y_pred'],
            x_pred=plot_df[feature_col], # Pass the sorted X values for the line
            output_dir=output_dir,
            title=f"Logarithmic Fit: {target_col} vs {feature_col}",
            file_name=f"log_regression_{target_col}_vs_{feature_col}.png"
        )

        return { "regression_plot": plot_filename }

    def _save_to_excel(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the original data points used for fitting and the corresponding predictions.
        """
        # 从model_params中获取参数
        model_params = computed_data.get('model_params', {})
        feature_cols = model_params.get('feature_columns')
        target_col = model_params.get('target_column')

        if not feature_cols or not target_col:
            return {}

        feature_col = feature_cols[0]
        sorted_x = computed_data.get('sorted_x')
        sorted_y_pred = computed_data.get('sorted_y_pred')

        if sorted_x is None or sorted_y_pred is None:
            return {}

        report_df = pd.DataFrame({
            feature_col: sorted_x,
            f'actual_{target_col}': np.interp(sorted_x, dataframe[feature_col].dropna(), dataframe[target_col].dropna()),
            'predicted_y': sorted_y_pred
        })
        
        file_name = f"log_regression_report_{target_col}_vs_{feature_col}.xlsx"
        output_path = output_dir / file_name

        report_df.to_excel(output_path, index=False)
        
        return { "report_file": file_name }

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the trained Linear Regression model to a file.
        """
        model = computed_data.get('model_object')
        if model is None:
            return {}
            
        file_name = "logarithmic_regression_model.joblib"
        output_path = output_dir / file_name
        
        dump(model, output_path)
        
        return { "model_file": file_name }

class Predictor(BasePredictorAlgorithm):
    """
    Predicts using a pre-trained Logarithmic Regression model.
    """
    def _load_model(self, model_path: Path):
        """Loads the saved Linear Regression model."""
        return load(model_path)

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        """Runs prediction on new data."""
        feature_col = self.params['feature_columns'][0]
        
        # Ensure input data is positive for log transformation
        X_new = dataframe[feature_col].values
        if np.any(X_new <= 0):
            raise ValueError("Prediction data contains non-positive values, cannot apply log transformation.")
            
        X_new_log = np.log(X_new).reshape(-1, 1)
        
        predictions = model_artifact.predict(X_new_log)
        return pd.Series(predictions, name='predicted_y') 