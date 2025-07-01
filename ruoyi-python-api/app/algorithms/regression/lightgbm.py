# app/algorithms/regression/lightgbm.py
import pandas as pd
import numpy as np
import lightgbm as lgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import r2_score, mean_squared_error, mean_absolute_error
from pathlib import Path
from joblib import dump, load
import logging

# 设置matplotlib后端
import matplotlib
matplotlib.use('Agg')

# --- 关键路径修复 ---
# from python.algorithms.base_algorithm import BaseAlgorithm -> from app.algorithms.base_algorithm import BaseAlgorithm
# from python.visualizations.plot_factory import plot_regression -> from app.visualizations.plot_factory import plot_regression
# from python.algorithms.base_predictor import BasePredictorAlgorithm -> from app.algorithms.base_predictor import BasePredictorAlgorithm
from app.algorithms.base_algorithm import BaseAlgorithm
from app.visualizations.plot_factory import plot_regression
from app.algorithms.base_predictor import BasePredictorAlgorithm
from app.utils.data_cleaner import clean_regression_data, validate_data_for_ml
# --- 修复结束 ---

class Trainer(BaseAlgorithm):
    """
    Performs LightGBM Regression on given data.
    """
    def validate_params(self):
        """
        Validates parameters for LightGBM Regression.
        """
        if 'feature_columns' not in self.params:
            raise ValueError("Parameter 'feature_columns' is required.")
        if 'target_column' not in self.params:
            raise ValueError("Parameter 'target_column' is required.")

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Trains a LightGBM Regressor model and returns evaluation metrics.
        """
        feature_cols = self.params['feature_columns']
        if isinstance(feature_cols, str):
            feature_cols = [feature_cols]
            
        target_col = self.params['target_column']
        test_size = self.params.get('test_size', 0.2)
        random_state = self.params.get('random_state', 42)

        # LightGBM specific params
        n_estimators = int(self.params.get('n_estimators', 100))
        learning_rate = float(self.params.get('learning_rate', 0.1))

        # Handle max_depth parameter
        max_depth = self.params.get('max_depth')
        if max_depth and max_depth != 'None':
            max_depth = int(max_depth)
        else:
            max_depth = -1  # LightGBM default for unlimited depth

        # Handle num_leaves parameter
        num_leaves = self.params.get('num_leaves')
        if num_leaves and num_leaves != 'None':
            num_leaves = int(num_leaves)
        else:
            num_leaves = 31  # LightGBM default

        # 使用数据清理工具清理数据
        combined_df, X_df, y_series = clean_regression_data(
            dataframe, feature_cols, target_col,
            remove_outliers=True, outlier_percentile=99.9
        )

        # 转换为numpy数组
        X = X_df.values
        y = y_series.values

        # 验证数据质量
        validate_data_for_ml(X, y)
        
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=test_size, random_state=random_state
        )

        # Train model
        model = lgb.LGBMRegressor(
            objective='regression',
            n_estimators=n_estimators,
            learning_rate=learning_rate,
            max_depth=max_depth,
            num_leaves=num_leaves,
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

        # Calculate feature importance
        feature_importance = dict(zip(feature_cols, model.feature_importances_))

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
                "max_depth": int(max_depth) if max_depth != -1 else None,
                "num_leaves": int(num_leaves),
                "feature_columns": feature_cols,
                "target_column": target_col
            },
            "model_object": model,
            # 标准数据字段
            "predictions": y_pred,
            "actual_values": y_test,
            "feature_values": X_test,
            "feature_importance": feature_importance,
            "input_sample": dataframe[feature_cols + [target_col]].head(100).to_dict('records')
        }

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Generates a regression plot. Assumes the first feature column for the x-axis.
        """
        try:
            logging.info("开始生成LightGBM可视化图表...")

            # 从model_params中获取参数
            model_params = computed_data.get('model_params', {})
            feature_cols = model_params.get('feature_columns')
            target_col = model_params.get('target_column')

            if not feature_cols or not target_col:
                logging.warning("没有特征列或目标列，跳过可视化")
                return {}

            x_col = feature_cols[0]
            y_col = target_col

            logging.info(f"生成回归图: {y_col} vs {x_col}")

            # 获取预测数据
            predictions = computed_data.get('predictions')
            if predictions is None:
                logging.warning("没有预测数据，跳过可视化")
                return {}

            # 获取测试集的特征值和预测值（长度应该匹配）
            feature_values = computed_data.get('feature_values')

            if feature_values is None:
                logging.warning("没有特征值数据，跳过可视化")
                return {}

            # 确保使用测试集的特征值，而不是完整数据集
            if len(feature_values) > 0 and len(feature_values[0]) > 0:
                # feature_values是二维数组，取第一列作为x值
                x_values = [row[0] for row in feature_values]
            else:
                logging.warning("特征值数据格式错误，跳过可视化")
                return {}

            # Sort values for a clean line plot
            plot_df = pd.DataFrame({
                'x': x_values,
                'y_pred': predictions
            }).sort_values(by='x')

            logging.info(f"输出目录: {output_dir}")

            plot_filename = plot_regression(
                dataframe=dataframe,
                x_col=x_col,
                y_col=y_col,
                y_pred=plot_df['y_pred'], # <-- FIX: Was plot_df['y'], should be plot_df['y_pred']
                x_pred=plot_df['x'], # Pass sorted x for line
                output_dir=output_dir,
                title=f"LightGBM Fit: {y_col} vs {x_col}",
                file_name=f"lgbm_regression_{y_col}_vs_{x_col}.png"
            )

            logging.info(f"可视化图表生成成功: {plot_filename}")

            return {
                "regression_plot": plot_filename
            }

        except Exception as e:
            logging.error(f"生成可视化图表失败: {e}", exc_info=True)
            return {}

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
        
        file_name = f"lgbm_regression_report_{target_col}.xlsx"
        output_path = output_dir / file_name

        full_report_df.to_excel(output_path, index=False)
        
        return {
            "report_file": file_name
        }

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the trained LightGBM model to a file.
        """
        model = computed_data.get('model_object')
        if model is None:
            return {}
            
        file_name = "lightgbm_regression_model.joblib"
        output_path = output_dir / file_name
        
        dump(model, output_path)
        
        return { "model_file": file_name }

class Predictor(BasePredictorAlgorithm):
    """
    Predicts using a pre-trained LightGBM model.
    """
    def _load_model(self, model_path: Path):
        """Loads the saved LightGBM model."""
        return load(model_path)

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        """Runs prediction on new data."""
        feature_cols = self.params.get('feature_columns')
        if not feature_cols:
             raise ValueError("Feature columns must be specified for prediction.")

        X_new = dataframe[feature_cols].values
        
        predictions = model_artifact.predict(X_new)
        return pd.Series(predictions, name='predicted_y')
