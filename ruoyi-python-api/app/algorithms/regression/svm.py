# app/algorithms/regression/svm.py
import pandas as pd
from sklearn.svm import SVR
from sklearn.model_selection import train_test_split
from sklearn.metrics import r2_score, mean_squared_error, mean_absolute_error
from sklearn.preprocessing import StandardScaler
import numpy as np
from pathlib import Path
from joblib import dump, load

from app.algorithms.base_algorithm import BaseAlgorithm
from app.visualizations.plot_factory import plot_regression
from app.algorithms.base_predictor import BasePredictorAlgorithm
from app.utils.data_cleaner import clean_regression_data, validate_data_for_ml

class Trainer(BaseAlgorithm):
    """
    Performs Support Vector Machine (SVM) Regression on given data.
    """
    def validate_params(self):
        """
        Validates parameters for SVM Regression.
        """
        if 'feature_columns' not in self.params:
            raise ValueError("Parameter 'feature_columns' is required.")
        if 'target_column' not in self.params:
            raise ValueError("Parameter 'target_column' is required.")

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Trains an SVR model and returns evaluation metrics.
        """
        feature_cols = self.params['feature_columns']
        if isinstance(feature_cols, str):
            feature_cols = [feature_cols]
            
        target_col = self.params['target_column']
        test_size = self.params.get('test_size', 0.2)
        random_state = self.params.get('random_state', 42)

        # SVR specific params
        kernel = self.params.get('kernel', 'rbf')
        C = float(self.params.get('C', 1.0))

        # 使用数据清理工具清理数据
        _, X_df, y_series = clean_regression_data(
            dataframe, feature_cols, target_col,
            remove_outliers=True, outlier_percentile=99.9
        )

        # 转换为numpy数组
        X = X_df.values
        y = y_series.values

        # 验证数据质量
        validate_data_for_ml(X, y)
        
        # Scale the features
        scaler_X = StandardScaler()
        X_scaled = scaler_X.fit_transform(X)

        # Scale the target variable as well, especially for SVR
        scaler_y = StandardScaler()
        y_scaled = scaler_y.fit_transform(y.reshape(-1, 1)).ravel()

        X_train, X_test, y_train, y_test = train_test_split(
            X_scaled, y_scaled, test_size=test_size, random_state=random_state
        )

        # Train model
        model = SVR(kernel=kernel, C=C)
        model.fit(X_train, y_train)

        # Make predictions on scaled test data
        y_pred_scaled = model.predict(X_test)

        # Inverse transform the predictions and the test set to original scale for metric calculation
        y_pred = scaler_y.inverse_transform(y_pred_scaled.reshape(-1, 1)).flatten()
        y_test_orig = scaler_y.inverse_transform(y_test.reshape(-1, 1)).flatten()

        # Calculate metrics
        r2 = r2_score(y_test_orig, y_pred)
        mse = mean_squared_error(y_test_orig, y_pred)
        mae = mean_absolute_error(y_test_orig, y_pred)
        
        # Predict on the full scaled dataset for visualization
        full_pred_scaled = model.predict(X_scaled)
        full_pred = scaler_y.inverse_transform(full_pred_scaled.reshape(-1, 1)).flatten()

        return {
            "statistics": {
                "r2_score": float(r2),
                "mean_squared_error": float(mse),
                "mean_absolute_error": float(mae),
                "rmse": float(mse ** 0.5)
            },
            "model_params": {
                "kernel": kernel,
                "C": float(C),
                "feature_columns": feature_cols,
                "target_column": target_col
            },
            "model_object": model,
            "scaler_X": scaler_X,
            "scaler_y": scaler_y,
            # 标准数据字段
            "predictions": y_pred,
            "actual_values": y_test_orig,
            "feature_values": scaler_X.inverse_transform(X_test),
            "input_sample": dataframe[feature_cols + [target_col]].head(100).to_dict('records')
        }

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Generates a regression plot. Assumes the first feature column for the x-axis.
        """
        # 从model_params中获取参数
        model_params = computed_data.get('model_params', {})
        feature_cols = model_params.get('feature_columns')
        target_col = model_params.get('target_column')

        if not feature_cols or not target_col:
            return {}

        x_col = feature_cols[0]
        y_col = target_col
        
        # 获取预测数据
        predictions = computed_data.get('predictions')
        if predictions is None:
            return {}

        # 获取测试集的特征值和预测值（长度应该匹配）
        feature_values = computed_data.get('feature_values')

        if feature_values is None:
            return {}

        # 确保使用测试集的特征值，而不是完整数据集
        if len(feature_values) > 0 and len(feature_values[0]) > 0:
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
            x_col=x_col,
            y_col=y_col,
            y_pred=plot_df['y_pred'],
            x_pred=plot_df['x'], # Pass sorted x for line
            output_dir=output_dir,
            title=f"SVR Fit: {y_col} vs {x_col}",
            file_name=f"svr_regression_{y_col}_vs_{x_col}.png"
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
        
        full_report_df = pd.concat([x_test_df, report_df], axis=1)
        
        file_name = f"svr_regression_report_{target_col}.xlsx"
        output_path = output_dir / file_name

        full_report_df.to_excel(output_path, index=False)
        
        return {
            "report_file": file_name
        }

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the trained SVR model and scalers to a file.
        """
        model = computed_data.get('model_object')
        scaler_X = computed_data.get('scaler_X')
        scaler_y = computed_data.get('scaler_y')
        
        if model is None or scaler_X is None or scaler_y is None:
            return {}
            
        model_package = {
            'model': model,
            'scaler_X': scaler_X,
            'scaler_y': scaler_y
        }
            
        file_name = "svm_regression_model.joblib"
        output_path = output_dir / file_name
        
        dump(model_package, output_path)
        
        return { "model_file": file_name }

class Predictor(BasePredictorAlgorithm):
    """
    Predicts using a pre-trained SVR model.
    """
    def _load_model(self, model_path: Path):
        """Loads the saved SVR model and scalers."""
        return load(model_path)

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        """Runs prediction on new data."""
        feature_cols = self.params.get('feature_columns')
        if not feature_cols:
             raise ValueError("Feature columns must be specified for prediction.")

        model = model_artifact['model']
        scaler_X = model_artifact['scaler_X']
        scaler_y = model_artifact['scaler_y']

        X_new = dataframe[feature_cols].values
        X_new_scaled = scaler_X.transform(X_new)
        
        predictions_scaled = model.predict(X_new_scaled)
        predictions = scaler_y.inverse_transform(predictions_scaled.reshape(-1, 1)).flatten()
        
        return pd.Series(predictions, name='predicted_y') 
