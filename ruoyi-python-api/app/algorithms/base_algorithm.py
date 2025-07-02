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
            return self._read_excel_file()

        elif self.file_path.suffix == '.csv':
            # 支持CSV文件
            return pd.read_csv(self.file_path)

        # Add support for other file types like .las if needed
        # elif self.file_path.suffix == '.las':
        #     import lasio
        #     return lasio.read(self.file_path).df()
        else:
            raise ValueError(f"Unsupported file type: {self.file_path.suffix}")

    def _read_excel_file(self):
        """读取Excel文件，支持多工作表"""
        import zipfile

        # 首先检查文件是否真的是Excel格式
        try:
            # 尝试作为zip文件打开（Excel文件本质上是zip文件）
            with zipfile.ZipFile(self.file_path, 'r') as zip_file:
                file_list = zip_file.namelist()
                # 检查是否包含Excel必需的文件
                required_files = ['xl/workbook.xml', '[Content_Types].xml']
                missing_files = [f for f in required_files if f not in file_list]

                if missing_files:
                    # 如果缺少必需文件，可能是损坏的Excel文件
                    # 尝试作为CSV读取
                    return self._try_read_as_csv()

        except zipfile.BadZipFile:
            # 文件不是有效的zip文件，可能是CSV文件被错误命名为xlsx
            return self._try_read_as_csv()

        # 如果文件格式检查通过，尝试读取Excel
        return self._read_excel_with_multiple_engines()

    def _try_read_as_csv(self):
        """尝试将文件作为CSV读取"""
        try:
            # 尝试作为CSV读取
            with open(self.file_path, 'r', encoding='utf-8') as f:
                first_line = f.readline()
                if ',' in first_line or '\t' in first_line:
                    separator = ',' if ',' in first_line else '\t'
                    return pd.read_csv(self.file_path, sep=separator)
        except Exception:
            pass

        # 尝试其他编码
        try:
            with open(self.file_path, 'r', encoding='gbk') as f:
                first_line = f.readline()
                if ',' in first_line or '\t' in first_line:
                    separator = ',' if ',' in first_line else '\t'
                    return pd.read_csv(self.file_path, sep=separator, encoding='gbk')
        except Exception:
            pass

        raise ValueError(f"文件 {self.file_path} 不是有效的Excel或CSV文件格式")

    def _read_excel_with_multiple_engines(self):
        """使用多种引擎尝试读取Excel文件"""

        # 首先尝试获取工作表信息
        sheet_names = None
        try:
            import openpyxl
            workbook = openpyxl.load_workbook(self.file_path, read_only=True)
            sheet_names = workbook.sheetnames
            workbook.close()
        except Exception:
            pass

        # 定义可能的工作表名称（优先级从高到低）
        preferred_sheets = ['测井数据', '数据', 'data', 'Data', 'Sheet1', '工作表1']

        # 如果有工作表信息，选择最合适的工作表
        target_sheet = None
        if sheet_names:
            # 优先选择包含石油参数的工作表
            for sheet in preferred_sheets:
                if sheet in sheet_names:
                    target_sheet = sheet
                    break

            # 如果没找到首选工作表，使用第一个工作表
            if target_sheet is None:
                target_sheet = sheet_names[0]

        # 尝试不同的引擎读取
        engines = ['openpyxl', 'xlrd', None]  # None表示使用默认引擎

        for engine in engines:
            try:
                if target_sheet:
                    # 指定工作表读取
                    if engine:
                        return pd.read_excel(self.file_path, sheet_name=target_sheet, engine=engine)
                    else:
                        return pd.read_excel(self.file_path, sheet_name=target_sheet)
                else:
                    # 不指定工作表，使用默认
                    if engine:
                        return pd.read_excel(self.file_path, engine=engine)
                    else:
                        return pd.read_excel(self.file_path)

            except Exception as e:
                last_error = e
                continue

        # 如果所有方法都失败，抛出详细错误信息
        error_msg = f"无法读取Excel文件 {self.file_path}。\n"
        if sheet_names:
            error_msg += f"工作表: {sheet_names}\n"
        error_msg += f"最后错误: {str(last_error)}\n"
        error_msg += "建议：请检查文件是否损坏，或尝试重新生成Excel文件。"
        raise ValueError(error_msg)


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