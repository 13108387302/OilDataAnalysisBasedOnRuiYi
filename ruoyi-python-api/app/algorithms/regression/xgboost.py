# app/algorithms/regression/xgboost.py
import pandas as pd
import xgboost as xgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import r2_score, mean_squared_error, mean_absolute_error
from pathlib import Path
from joblib import dump, load

from app.algorithms.base_algorithm import BaseAlgorithm
from app.visualizations.plot_factory import plot_regression
from app.algorithms.base_predictor import BasePredictorAlgorithm

class Trainer(BaseAlgorithm):
    """
    Performs XGBoost Regression on given data.
    """
    def validate_params(self):
        """
        Validates parameters for XGBoost Regression.
        """
        if 'feature_columns' not in self.params:
            raise ValueError("Parameter 'feature_columns' is required.")
        if 'target_column' not in self.params:
            raise ValueError("Parameter 'target_column' is required.")

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Trains an XGBoost Regressor model and returns evaluation metrics.
        """
        feature_cols = self.params['feature_columns']
        if isinstance(feature_cols, str):
            feature_cols = [feature_cols]

        target_col = self.params['target_column']
        test_size = self.params.get('test_size', 0.2)
        random_state = self.params.get('random_state', 42)

        # XGBoost specific params
        n_estimators = int(self.params.get('n_estimators', 100))
        learning_rate = float(self.params.get('learning_rate', 0.1))
        max_depth = int(self.params.get('max_depth', 3))

        # Prepare data
        X = dataframe[feature_cols].values
        y = dataframe[target_col].values
        
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=test_size, random_state=random_state
        )

        # Train model
        model = xgb.XGBRegressor(
            objective='reg:squarederror',
            n_estimators=n_estimators,
            learning_rate=learning_rate,
            max_depth=max_depth,
            random_state=random_state,
            n_jobs=-1
        )
        model.fit(X_train, y_train)

        # Make predictions
        y_pred = model.predict(X_test)

        # Calculate metrics
        r2 = r2_score(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)
        mae = mean_absolute_error(y_test, y_pred)
        
        # Predict on the full dataset for visualization
        full_pred = model.predict(X)

        return {
            "statistics": {
                "r2_score": float(r2),
                "mean_squared_error": float(mse),
                "mean_absolute_error": float(mae),
                "rmse": float(mse ** 0.5)
            },
            "model_params": {
                "n_estimators": int(n_estimators),
                "learning_rate": float(learning_rate),
                "max_depth": int(max_depth),
                "feature_columns": feature_cols,
                "target_column": target_col
            },
            "model_object": model,
            # 标准数据字段
            "predictions": y_pred,
            "actual_values": y_test,
            "feature_values": X_test,
            "feature_importance": dict(zip(feature_cols, model.feature_importances_)),
            "input_sample": dataframe[feature_cols + [target_col]].head(100).to_dict('records')
        }

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Generates a regression plot. Assumes the first feature column for the x-axis.
        """
        # 从model_params中获取特征列信息
        model_params = computed_data.get('model_params', {})
        feature_cols = model_params.get('feature_columns')
        if not feature_cols:
            return {}

        x_col = feature_cols[0]
        y_col = model_params.get('target_column')
        
        # 获取测试集的特征值和预测值（长度应该匹配）
        feature_values = computed_data.get('feature_values')
        predictions = computed_data.get('predictions')

        if feature_values is None or predictions is None:
            return {}

        # 确保使用测试集的特征值，而不是完整数据集
        if len(feature_values) > 0 and len(feature_values[0]) > 0:
            # feature_values是二维数组，取第一列作为x值
            x_values = [row[0] for row in feature_values]
        else:
            return {}

        # For a smooth curve, sort the values by X before plotting
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
            title=f"XGBoost Fit: {y_col} vs {x_col}",
            file_name=f"xgb_regression_{y_col}_vs_{x_col}.png"
        )

        return {
            "regression_plot": plot_filename
        }

    def _save_to_excel(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the test set data along with the model's predictions to an Excel file.
        """
        # 从model_params中获取特征列信息
        model_params = computed_data.get('model_params', {})
        feature_cols = model_params.get('feature_columns')
        target_col = model_params.get('target_column')
        if not feature_cols or not target_col:
            return {}

        # 使用标准化字段名
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
        
        file_name = f"xgb_regression_report_{target_col}.xlsx"
        output_path = output_dir / file_name

        full_report_df.to_excel(output_path, index=False)
        
        return {
            "report_file": file_name
        }

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the trained XGBoost model to a file.
        """
        model = computed_data.get('model_object')
        if model is None:
            return {}
            
        file_name = "xgboost_regression_model.joblib"
        output_path = output_dir / file_name
        
        dump(model, output_path)
        
        return { "model_file": file_name }

class Predictor(BasePredictorAlgorithm):
    """
    Predicts using a pre-trained XGBoost model.
    """
    def _load_model(self, model_path: Path):
        """Loads the saved XGBoost model."""
        return load(model_path)

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        """Runs prediction on new data."""
        feature_cols = self.params.get('feature_columns')
        if not feature_cols:
             raise ValueError("Feature columns must be specified for prediction.")

        X_new = dataframe[feature_cols].values
        
        predictions = model_artifact.predict(X_new)
        return pd.Series(predictions, name='predicted_y') 