# app/algorithms/regression/exponential.py
import pandas as pd
import numpy as np
from scipy.optimize import curve_fit
from sklearn.metrics import r2_score, mean_squared_error, mean_absolute_error
from pathlib import Path
import json

from app.algorithms.base_algorithm import BaseAlgorithm
from app.visualizations.plot_factory import plot_regression, plot_scatter
from app.algorithms.base_predictor import BasePredictorAlgorithm

# Define the exponential function form, accessible by both Trainer and Predictor
def exponential_func(x, a, b, c):
    return a * np.exp(b * x) + c

class Trainer(BaseAlgorithm):
    """
    Performs Exponential Regression using non-linear least squares.
    """
    def validate_params(self):
        """
        Validates parameters for Exponential Regression.
        """
        # Call parent validation first (handles string to list conversion)
        super().validate_params()

        # Exponential regression specific validation: must have exactly one feature column
        if len(self.params['feature_columns']) != 1:
            raise ValueError("Exponential regression requires exactly one feature column.")

    def _create_debug_scatter_plot(self, dataframe: pd.DataFrame, output_dir_for_failure: Path) -> str:
        """
        Creates a scatter plot for debugging when curve fitting fails.
        """
        try:
            debug_plot_filename = plot_scatter(
                dataframe=dataframe,
                x_col=self.params['feature_columns'][0],
                y_col=self.params['target_column'],
                output_dir=output_dir_for_failure,
                title=f"Debug Scatter Plot for Failed Fit",
                file_name=f"debug_scatter_{self.params['target_column']}_vs_{self.params['feature_columns'][0]}.png"
            )
            return debug_plot_filename
        except Exception as e:
            return f"Failed to create debug plot: {str(e)}"

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Fits an exponential model and returns evaluation metrics.
        """
        feature_col = self.params['feature_columns'][0]
        target_col = self.params['target_column']

        clean_df = dataframe[[feature_col, target_col]].dropna().sort_values(by=feature_col)

        if len(clean_df) < 3:
            raise ValueError("数据点不足（少于3个），无法执行指数回归。")

        X_orig = clean_df[feature_col].values
        y_orig = clean_df[target_col].values
        
        x_range = X_orig.max() - X_orig.min()
        y_range = y_orig.max() - y_orig.min()

        if x_range == 0:
            raise ValueError(f"无法执行回归分析，因为自变量 '{feature_col}' 的所有数值都相同。")
        if y_range == 0:
            raise ValueError(f"无法执行回归分析，因为因变量 '{target_col}' 的所有数值都相同。")

        x_scaled = (X_orig - X_orig.min()) / x_range
        y_scaled = (y_orig - y_orig.min()) / y_range
        
        c_guess_scaled = y_scaled[0]
        a_guess_scaled = y_scaled.max() - y_scaled.min()
        b_guess_scaled = 0.1
        
        initial_guesses = [a_guess_scaled, b_guess_scaled, c_guess_scaled]
        bounds_scaled = ([0, -np.inf, 0], [1, np.inf, 1])

        try:
            params_scaled, _ = curve_fit(
                exponential_func, x_scaled, y_scaled, 
                p0=initial_guesses, bounds=bounds_scaled, maxfev=10000
            )
            
            a_unscaled = params_scaled[0] * y_range
            b_unscaled = params_scaled[1] / x_range
            c_unscaled = params_scaled[2] * y_range + y_orig.min()
            
            final_params, _ = curve_fit(
                exponential_func, X_orig, y_orig,
                p0=[a_unscaled, b_unscaled, c_unscaled], maxfev=5000
            )
        except RuntimeError:
            debug_plot_filename = self._create_debug_scatter_plot(dataframe, self.output_dir)
            error_message = (
                f"无法为数据 '{target_col}' vs '{feature_col}' 找到合适的指数函数模型。"
                "这通常意味着您选择的数据不呈指数关系。"
                "为了帮助您诊断，我们在结果目录中生成了一张数据散点图，请查看它以直观地了解数据分布。"
                f"散点图文件: '{debug_plot_filename}'"
            )
            raise RuntimeError(error_message)

        a, b, c = final_params
        y_pred = exponential_func(X_orig, a, b, c)
        r2 = r2_score(y_orig, y_pred)
        
        full_x = dataframe[feature_col].dropna().sort_values()
        full_pred = exponential_func(full_x, a, b, c)

        # 确保数据格式一致性 - 使用与其他算法相同的数据结构
        # 使用完整数据集进行可视化，但确保数据格式正确
        clean_full_df = dataframe[[feature_col, target_col]].dropna().sort_values(by=feature_col)

        return {
            "statistics": {
                "r2_score": float(r2),
                "equation": f"y = {a:.4f} * exp({b:.4f} * x) + {c:.4f}",
                "mean_squared_error": float(mean_squared_error(y_orig, y_pred)),
                "mean_absolute_error": float(mean_absolute_error(y_orig, y_pred)),
                "rmse": float(np.sqrt(mean_squared_error(y_orig, y_pred)))
            },
            "model_params": {
                "a": float(a),
                "b": float(b),
                "c": float(c),
                "feature_columns": self.params['feature_columns'],
                "target_column": target_col
            },
            "sorted_x": self.convert_to_serializable(full_x),
            "sorted_y_pred": self.convert_to_serializable(full_pred),
            "feature_columns": self.params['feature_columns'],
            "target_column": target_col,
            # 标准数据字段 - 确保与其他算法格式一致
            "predictions": self.convert_to_serializable(full_pred.tolist() if hasattr(full_pred, 'tolist') else full_pred),
            "feature_values": [[float(x)] for x in (full_x.tolist() if hasattr(full_x, 'tolist') else full_x)],  # 转换为二维数组格式
            "actual_values": self.convert_to_serializable(clean_full_df[target_col].tolist()),
            "input_sample": dataframe[self.params['feature_columns'] + [target_col]].head(100).to_dict('records')
        }

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        feature_col = computed_data['feature_columns'][0]
        target_col = computed_data['target_column']
        
        plot_df = pd.DataFrame({
            feature_col: computed_data['sorted_x'],
            'y_pred': computed_data['sorted_y_pred']
        })
        
        plot_filename = plot_regression(
            dataframe=dataframe, x_col=feature_col, y_col=target_col,
            y_pred=plot_df['y_pred'], x_pred=plot_df[feature_col],
            output_dir=output_dir, title=f"Exponential Fit: {target_col} vs {feature_col}",
            file_name=f"exp_regression_{target_col}_vs_{feature_col}.png"
        )
        return {"regression_plot": plot_filename}

    def _save_to_excel(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        feature_col = computed_data['feature_columns'][0]
        target_col = computed_data['target_column']
        
        report_df = pd.DataFrame({
            feature_col: computed_data['sorted_x'],
            f'actual_{target_col}': np.interp(computed_data['sorted_x'], dataframe[feature_col].dropna(), dataframe[target_col].dropna()),
            'predicted_y': computed_data['sorted_y_pred']
        })
        
        file_name = f"exp_regression_report_{target_col}_vs_{feature_col}.xlsx"
        output_path = output_dir / file_name
        report_df.to_excel(output_path, index=False)
        return {"report_file": file_name}

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the fitted model parameters to a JSON file.
        """
        model_params = computed_data.get('model_params')
        if not model_params:
            return {}
        
        file_name = "exponential_regression_model.json"
        output_path = output_dir / file_name
        
        with open(output_path, 'w') as f:
            json.dump(model_params, f, indent=4)
            
        return {"model_file": file_name}

class Predictor(BasePredictorAlgorithm):
    """
    Predicts using pre-calculated exponential function parameters.
    """
    def _load_model(self, model_path: Path):
        """Loads the saved model parameters from a JSON file."""
        with open(model_path, 'r') as f:
            return json.load(f)

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        """Runs prediction on new data using the loaded parameters."""
        a = model_artifact['a']
        b = model_artifact['b']
        c = model_artifact['c']
        
        feature_col = self.params['feature_columns'][0]
        X_new = dataframe[feature_col].values
        
        predictions = exponential_func(X_new, a, b, c)
        return pd.Series(predictions, name='predicted_y') 