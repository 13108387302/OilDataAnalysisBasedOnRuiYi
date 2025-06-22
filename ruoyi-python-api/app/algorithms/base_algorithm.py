import pandas as pd
from pathlib import Path
import sys
import traceback
import numpy as np

class BaseAlgorithm:
    """
    Base class for all analysis algorithms.
    Uses the Template Method design pattern.
    """
    def __init__(self, file_path: str, output_dir: str, params: dict):
        self.file_path = Path(file_path)
        self.output_dir = Path(output_dir)
        self.params = params
        
        # Common ML parameters with defaults
        self.test_size = float(self.params.get('test_size', 0.2))
        self.random_state = int(self.params.get('random_state', 42))

        # Create output directory if it doesn't exist
        self.output_dir.mkdir(parents=True, exist_ok=True)

    @staticmethod
    def convert_to_serializable(obj):
        """
        将NumPy数据类型转换为Python原生类型，确保可以序列化为JSON
        """
        if isinstance(obj, np.integer):
            return int(obj)
        elif isinstance(obj, np.floating):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        elif isinstance(obj, dict):
            return {k: BaseAlgorithm.convert_to_serializable(v) for k, v in obj.items()}
        elif isinstance(obj, list):
            return [BaseAlgorithm.convert_to_serializable(item) for item in obj]
        else:
            return obj


    def validate_params(self):
        """
        Validates common parameters like 'feature_columns' and 'target_column'.
        Converts 'feature_columns' from a string to a list if necessary.
        Subclasses should call super().validate_params() and then add their specific checks.
        """
        # --- Common Parameter Validation ---

        # 1. Validate 'target_column'
        if 'target_column' not in self.params or not isinstance(self.params.get('target_column'), str):
            raise ValueError("Parameter 'target_column' is required and must be a string.")

        # 2. Validate and normalize 'feature_columns'
        feature_columns = self.params.get('feature_columns')
        if not feature_columns:
            raise ValueError("Parameter 'feature_columns' is required.")

        # If it's a string, convert it to a single-element list for backward compatibility
        if isinstance(feature_columns, str):
            self.params['feature_columns'] = [feature_columns]

        # After potential conversion, ensure it's a non-empty list of strings
        if not isinstance(self.params['feature_columns'], list) or not self.params['feature_columns']:
            raise ValueError("'feature_columns' must be a non-empty list of column names.")
    
    def _load_data(self) -> pd.DataFrame:
        """
        Loads data from the specified file path.
        Supports .xlsx and .las files.
        """
        if not self.file_path.exists():
            raise FileNotFoundError(f"Input file not found at: {self.file_path}")

        if self.file_path.suffix == '.xlsx':
            return pd.read_excel(self.file_path)
        # Add support for other file types like .las if needed
        # elif self.file_path.suffix == '.las':
        #     import lasio
        #     return lasio.read(self.file_path).df()
        else:
            raise ValueError(f"Unsupported file type: {self.file_path.suffix}")


    def run(self) -> dict:
        """
        A template method that defines the standard workflow for an algorithm.
        1. Validates input parameters.
        2. Computes the core logic.
        3. Generates visualizations.
        4. Saves key results to an Excel file.
        5. Formats the final JSON output.
        """
        self.validate_params()
        
        try:
            dataframe = self._load_data()

            computed_data = self._compute(dataframe)
            visualizations = self._visualize(dataframe, computed_data, self.output_dir)
            
            excel_report = self._save_to_excel(dataframe, computed_data, self.output_dir)
            model_artifact = self._save_model(computed_data, self.output_dir)

            return self._format_output(computed_data, visualizations, excel_report, model_artifact)

        except Exception as e:
            traceback.print_exc(file=sys.stderr)
            raise e

    def _compute(self, dataframe: pd.DataFrame):
        """
        Core computation logic. Must be implemented by subclasses.
        """
        raise NotImplementedError("Subclasses must implement the '_compute' method.")

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Default implementation for visualization. Returns an empty dict.
        Subclasses should override this to generate plots.
        """
        return {}

    def _save_to_excel(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Default implementation for saving to excel. Returns empty dict.
        """
        return {}

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """
        Default implementation for saving the trained model. Returns empty dict.
        """
        return {}

    def _format_output(self, computed_data: dict, visualizations: dict, excel_report: dict, model_artifact: dict) -> dict:
        """
        Formats the final output dictionary to be saved as result.json.
        Enhanced to include more data for advanced visualization with unified field names.
        """
        # 基础输出结构 - 优先使用statistics，如果没有则使用metrics作为后备
        statistics = computed_data.get("statistics")
        if statistics is None:
            statistics = computed_data.get("metrics", {})

        output = {
            "statistics": self.convert_to_serializable(statistics),
            "model_params": self.convert_to_serializable(computed_data.get("model_params", {})),
            "visualizations": visualizations,
            "excel_report": excel_report,
            "model_artifact": model_artifact
        }

        # 直接复制computed_data中的其他字段到output中
        for key, value in computed_data.items():
            if key not in output and value is not None:
                output[key] = self.convert_to_serializable(value)

        # 应用标准化格式
        return self._standardize_output_format(output)

    def _standardize_output_format(self, output: dict) -> dict:
        """
        标准化输出格式，确保所有算法返回统一的数据结构
        """
        # 确保基础结构存在
        standardized = {
            "algorithm_type": self._get_algorithm_type(),
            "algorithm_name": self.__class__.__name__,
            "statistics": output.get("statistics", {}),
            "metrics": output.get("statistics", {}),  # 兼容性别名
            "model_params": output.get("model_params", {}),
            "visualizations": output.get("visualizations", {}),
            "excel_report": output.get("excel_report", {}),
            "model_artifact": output.get("model_artifact", {})
        }

        # 标准化数据字段
        self._standardize_data_fields(output, standardized)

        # 标准化算法特定字段
        self._standardize_algorithm_specific_fields(output, standardized)

        return standardized

    def _get_algorithm_type(self) -> str:
        """
        获取算法类型，子类可以重写此方法
        """
        # 优先检查模块路径
        module_path = self.__class__.__module__.lower()

        if 'regression' in module_path:
            return 'regression'
        elif 'classification' in module_path:
            return 'classification'
        elif 'clustering' in module_path:
            return 'clustering'
        elif 'feature_engineering' in module_path:
            return 'feature_engineering'
        elif 'time_series' in module_path:
            return 'time_series'

        # 如果模块路径不明确，检查类名
        class_name = self.__class__.__name__.lower()

        if any(keyword in class_name for keyword in ['regression', 'xgboost', 'lightgbm', 'bilstm']):
            return 'regression'
        elif any(keyword in class_name for keyword in ['classification', 'logistic', 'svm_classification']):
            return 'classification'
        elif any(keyword in class_name for keyword in ['clustering', 'kmeans', 'dbscan']):
            return 'clustering'
        elif any(keyword in class_name for keyword in ['fractal', 'feature_engineering']):
            return 'feature_engineering'
        elif any(keyword in class_name for keyword in ['lstm', 'tcn', 'arima']):
            return 'time_series'
        else:
            return 'generic'

    def _standardize_data_fields(self, source: dict, target: dict):
        """
        标准化通用数据字段
        """
        # 1. 预测数据 - 统一使用 "predictions"
        predictions = None
        for key in ['predictions', 'y_pred', 'cluster_labels']:
            if key in source and source[key] is not None:
                predictions = source[key]
                break

        if predictions is not None:
            target["predictions"] = self.convert_to_serializable(predictions)

        # 2. 实际值 - 统一使用 "actual_values"
        actual_values = None
        for key in ['actual_values', 'y_test', 'y_true']:
            if key in source and source[key] is not None:
                actual_values = source[key]
                break

        if actual_values is not None:
            target["actual_values"] = self.convert_to_serializable(actual_values)

        # 3. 特征值 - 统一使用 "feature_values"
        feature_values = None
        for key in ['feature_values', 'X_test', 'X_scaled', 'depth_values']:
            if key in source and source[key] is not None:
                feature_values = source[key]
                break

        if feature_values is not None:
            target["feature_values"] = self.convert_to_serializable(feature_values)

        # 4. 特征重要性
        if 'feature_importance' in source:
            target["feature_importance"] = self.convert_to_serializable(source['feature_importance'])

        # 5. 输入样本
        input_sample = None
        for key in ['input_sample', 'input_data']:
            if key in source and source[key] is not None:
                input_sample = source[key]
                break

        if input_sample is not None:
            target["input_sample"] = self.convert_to_serializable(input_sample)

    def _standardize_algorithm_specific_fields(self, source: dict, target: dict):
        """
        标准化算法特定字段，子类可以重写此方法
        """
        # 聚类相关
        for key in ['cluster_labels', 'cluster_centers', 'cluster_sizes']:
            if key in source:
                target[key] = self.convert_to_serializable(source[key])

        # 分类相关
        for key in ['probabilities', 'y_prob', 'class_names', 'confusion_matrix']:
            if key in source:
                if key in ['probabilities', 'y_prob']:
                    target["probabilities"] = self.convert_to_serializable(source[key])
                else:
                    target[key] = self.convert_to_serializable(source[key])

        # 特征工程相关
        for key in ['fractal_dimension', 'correlation_coefficient', 'depth_values']:
            if key in source:
                target[key] = self.convert_to_serializable(source[key])

        # 时间序列相关
        if 'training_history' in source:
            target["training_history"] = self.convert_to_serializable(source['training_history'])