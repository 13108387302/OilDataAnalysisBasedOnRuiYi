import pandas as pd
import numpy as np
import importlib
from pathlib import Path

from app.algorithms.base_algorithm import BaseAlgorithm

# List of algorithms to try, referencing the new module structure.
CANDIDATE_ALGORITHMS = [
    {"name": "Linear Regression", "path": "app.algorithms.regression.linear", "params": {}},
    {"name": "Logarithmic Regression", "path": "app.algorithms.regression.logarithmic", "params": {}},
    {"name": "Polynomial Regression (Deg 2)", "path": "app.algorithms.regression.polynomial", "params": {"degree": 2}},
    {"name": "Polynomial Regression (Deg 3)", "path": "app.algorithms.regression.polynomial", "params": {"degree": 3}},
    {"name": "Exponential Regression", "path": "app.algorithms.regression.exponential", "params": {}},
]

class Trainer(BaseAlgorithm):
    """
    Automatically selects the best simple regression model from a list of candidates
    based on the highest R-squared value.
    """
    def validate_params(self):
        if 'feature_columns' not in self.params:
            raise ValueError("Parameter 'feature_columns' is required.")
        if 'target_column' not in self.params:
            raise ValueError("Parameter 'target_column' is required.")

    def _get_algorithm_trainer_class(self, path_string: str):
        """Dynamically imports the Trainer class from a module path."""
        module = importlib.import_module(path_string)
        return getattr(module, 'Trainer')

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        best_model_name = None
        best_r2 = -np.inf
        best_result = None
        best_instance = None
        
        all_results_summary = []

        for model_info in CANDIDATE_ALGORITHMS:
            try:
                current_params = self.params.copy()
                current_params.update(model_info["params"])

                AlgorithmTrainer = self._get_algorithm_trainer_class(model_info["path"])
                # We need to pass the output directory to the sub-algorithms
                instance = AlgorithmTrainer(self.file_path, self.output_dir, current_params)
                
                # The public `run` method handles validation and all steps.
                # Here we call the protected methods directly.
                instance.validate_params()
                computed_data = instance._compute(dataframe)
                
                # 优先从statistics获取，如果没有则从metrics获取
                statistics = computed_data.get("statistics", {})
                if not statistics:
                    statistics = computed_data.get("metrics", {})
                r2 = statistics.get("r2_score")
                
                if r2 is not None:
                    all_results_summary.append({"model": model_info["name"], "r2_score": r2})
                    if r2 > best_r2:
                        best_r2 = r2
                        best_model_name = model_info["name"]
                        best_result = computed_data
                        best_instance = instance
                else:
                    all_results_summary.append({"model": model_info["name"], "r2_score": "N/A"})

            except Exception as e:
                # Catch errors from sub-algorithms to not stop the whole process
                all_results_summary.append({"model": model_info["name"], "r2_score": f"Error: {e}"})

        if best_result is None:
            raise RuntimeError("Could not find any suitable regression model for the given data.")
            
        # 确保使用正确的字段名
        if "statistics" in best_result:
            best_result["statistics"]["best_model_selected"] = best_model_name
            best_result["statistics"]["all_models_tried"] = all_results_summary
        elif "metrics" in best_result:
            best_result["metrics"]["best_model_selected"] = best_model_name
            best_result["metrics"]["all_models_tried"] = all_results_summary

        # 增强可视化数据
        best_result["model_params"]["algorithm_type"] = "automatic_regression"
        best_result["model_params"]["best_model"] = best_model_name
        best_result["model_params"]["total_models_tested"] = len(CANDIDATE_ALGORITHMS)
        
        # Stash the winning instance and its results for other methods to use
        self.winning_instance = best_instance
        self.winning_computed_data = best_result

        return best_result

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """Calls the visualize method of the winning algorithm."""
        if hasattr(self, 'winning_instance') and self.winning_instance:
            return self.winning_instance._visualize(dataframe, self.winning_computed_data, output_dir)
        return {}

    def _save_to_excel(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """Calls the save_to_excel method of the winning algorithm."""
        if hasattr(self, 'winning_instance') and self.winning_instance:
            return self.winning_instance._save_to_excel(dataframe, self.winning_computed_data, output_dir)
        return {}

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """This meta-algorithm does not save a model itself."""
        return {} 