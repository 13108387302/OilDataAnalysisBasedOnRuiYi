# app/algorithms/regression/polynomial.py
import pandas as pd
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import PolynomialFeatures
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
    Performs Polynomial Regression on given data.
    """
    def validate_params(self):
        """
        Validates parameters for Polynomial Regression.
        """
        # First, run the base validation from the parent class
        super().validate_params()

        # Get the normalized feature_columns from the base class
        feature_columns = self.params.get('feature_columns')

        # For Polynomial Regression, we require exactly one feature column.
        if len(feature_columns) != 1:
            raise ValueError("Polynomial Regression requires exactly one feature column.")

        # Check for the 'degree' parameter, specific to Polynomial Regression
        if 'degree' not in self.params or not isinstance(self.params.get('degree'), int):
            raise ValueError("Integer parameter 'degree' is required for Polynomial Regression.")

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Trains a Polynomial Regression model and returns evaluation metrics.
        """
        feature_col = self.params['feature_columns'][0]
        target_col = self.params['target_column']
        degree = int(self.params.get('degree', 2))
        test_size = self.params.get('test_size', 0.2)
        random_state = self.params.get('random_state', 42)

        # Prepare data
        X = dataframe[[feature_col]].values
        y = dataframe[target_col].values
        
        # Create polynomial features
        poly = PolynomialFeatures(degree=degree)
        X_poly = poly.fit_transform(X)
        
        X_train, X_test, y_train, y_test = train_test_split(
            X_poly, y, test_size=test_size, random_state=random_state
        )

        # Train model on polynomial features
        model = LinearRegression()
        model.fit(X_train, y_train)

        # Make predictions
        y_pred = model.predict(X_test)

        # Calculate metrics
        r2 = r2_score(y_test, y_pred)
        mse = mean_squared_error(y_test, y_pred)
        mae = mean_absolute_error(y_test, y_pred)
        
        # 注释：full_pred 在当前实现中不需要，因为我们使用测试集进行可视化

        # 为了保持一致性，我们需要保存原始特征值而不是多项式扩展后的特征值
        # 从原始数据中获取测试集对应的原始特征值
        original_X = dataframe[[feature_col]].values
        _, original_X_test, _, _ = train_test_split(
            original_X, y, test_size=test_size, random_state=random_state
        )

        return {
            "statistics": {
                "r2_score": float(r2),
                "mean_squared_error": float(mse),
                "mean_absolute_error": float(mae),
                "rmse": float(mse ** 0.5)
            },
            "model_params": {
                "degree": int(degree),
                "coefficient_count": int(len(model.coef_)),
                "intercept": float(model.intercept_),
                "feature_columns": self.params['feature_columns'],
                "target_column": target_col
            },
            "model_object": model,
            "poly_features": poly,
            # 标准数据字段 - 使用原始特征值而不是多项式扩展后的特征值
            "predictions": y_pred,
            "actual_values": y_test,
            "feature_values": original_X_test,  # 使用原始特征值
            "poly_feature_values": X_test,      # 保存多项式特征值用于内部使用
            "coefficients": model.coef_,
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
        degree = model_params.get('degree')

        if not feature_cols or not target_col or degree is None:
            return {}

        feature_col = feature_cols[0]
        y_pred = computed_data.get('predictions')

        if y_pred is None:
            return {}

        # 获取测试集的特征值和预测值（长度应该匹配）
        feature_values = computed_data.get('feature_values')

        if feature_values is None:
            return {}

        # 确保使用测试集的特征值，而不是完整数据集
        if len(feature_values) > 0:
            # feature_values是二维数组，取第一列作为x值
            x_values = [row[0] for row in feature_values]
        else:
            return {}

        # For a smooth curve, sort the values by X before plotting
        plot_df = pd.DataFrame({'x': x_values, 'y_pred': y_pred}).sort_values(by='x')
        
        plot_filename = plot_regression(
            dataframe=dataframe,
            x_col=feature_col,
            y_col=target_col,
            y_pred=plot_df['y_pred'],
            x_pred=plot_df['x'],
            output_dir=output_dir,
            title=f"Polynomial Regression Fit (Degree {degree}): {target_col} vs {feature_col}",
            file_name=f"poly_regression_{target_col}_vs_{feature_col}.png"
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

        file_name = f"poly_regression_report_{target_col}.xlsx"
        output_path = output_dir / file_name

        full_report_df.to_excel(output_path, index=False)

        return {
            "report_file": file_name
        }

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the trained Polynomial Regression model and the feature transformer.
        """
        model = computed_data.get('model_object')
        poly = computed_data.get('poly_features')
        
        if model is None or poly is None:
            return {}
            
        file_name = "poly_regression_model.joblib"
        output_path = output_dir / file_name
        
        # Save the feature transformer and the model together as a tuple
        dump((poly, model), output_path)
        
        return { "model_file": file_name }

class Predictor(BasePredictorAlgorithm):
    """
    Predicts using a pre-trained Polynomial Regression model.
    """
    def _load_model(self, model_path: Path):
        """Loads the saved model tuple (poly_features, model)."""
        return load(model_path)

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        """Runs prediction on new data."""
        poly_features, model = model_artifact
        feature_col = self.params['feature_columns'][0]
        
        X_new = dataframe[[feature_col]].values
        X_new_poly = poly_features.transform(X_new)
        
        predictions = model.predict(X_new_poly)
        return pd.Series(predictions, name='predicted_y') 