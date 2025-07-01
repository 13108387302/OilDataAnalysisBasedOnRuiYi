# app/algorithms/base_predictor.py
import pandas as pd
from pathlib import Path
from app.utils.data_cleaner import clean_prediction_data, validate_prediction_data

class BasePredictorAlgorithm:
    """
    Base class for all prediction-only algorithms.
    It loads a pre-trained model and runs prediction on new data.
    """
    def __init__(self, file_path: str, output_dir: str, params: dict):
        self.file_path = Path(file_path)
        self.output_dir = Path(output_dir)
        self.params = params
        
        # Create output directory if it doesn't exist
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.validate_params()

    def validate_params(self):
        """Requires the path to the model artifact."""
        if 'model_path' not in self.params:
            raise ValueError("Parameter 'model_path' is required for prediction.")

    def _load_data(self) -> pd.DataFrame:
        """
        Loads data from the specified file path.
        Supports .xlsx and .las files.
        """
        if not self.file_path.exists():
            raise FileNotFoundError(f"Input file not found at: {self.file_path}")

        if self.file_path.suffix == '.xlsx':
            return pd.read_excel(self.file_path)
        else:
            raise ValueError(f"Unsupported file type: {self.file_path.suffix}")

    def run(self) -> dict:
        """Template method for prediction."""
        # The model path should be an absolute path or relative to a known location.
        # Here, we assume the path provided in params is complete.
        model_full_path = Path(self.params['model_path'])
        if not model_full_path.exists():
            raise FileNotFoundError(f"Model file not found at: {model_full_path}")

        model_artifact = self._load_model(model_full_path)
        raw_dataframe = self._load_data()

        # 🔧 处理行选择逻辑
        prediction_indices = self.params.get('prediction_indices')
        if prediction_indices is not None and len(prediction_indices) > 0:
            print(f"🔍 使用自定义行选择: {len(prediction_indices)} 个索引")
            print(f"🔍 索引范围: {prediction_indices[:10]}{'...' if len(prediction_indices) > 10 else ''}")

            # 验证索引范围
            max_index = len(raw_dataframe) - 1
            valid_indices = [idx for idx in prediction_indices if 0 <= idx <= max_index]
            if len(valid_indices) != len(prediction_indices):
                print(f"⚠️ 过滤无效索引: 原始{len(prediction_indices)}个，有效{len(valid_indices)}个")

            # 选择指定的行
            raw_dataframe = raw_dataframe.iloc[valid_indices].reset_index(drop=True)
            print(f"✅ 已选择 {len(raw_dataframe)} 行数据进行预测")
        else:
            print(f"🔍 使用全部数据: {len(raw_dataframe)} 行")

        # 获取特征列
        feature_cols = self.params.get('feature_columns')
        if not feature_cols:
            raise ValueError("Feature columns must be specified for prediction.")

        # 清理预测数据（只处理特征列，不需要目标列）
        print(f"🔧 开始清理预测数据，特征列: {feature_cols}")
        cleaned_dataframe, feature_dataframe = clean_prediction_data(
            raw_dataframe, feature_cols,
            remove_outliers=True, outlier_percentile=99.9
        )

        # 验证清理后的数据
        validate_prediction_data(feature_dataframe)

        # 使用清理后的数据进行预测
        predictions = self._predict(cleaned_dataframe, model_artifact)

        excel_report = self._save_to_excel(cleaned_dataframe, predictions, self.output_dir)

        return self._format_output(predictions, excel_report, cleaned_dataframe)

    def _load_model(self, model_path: Path):
        """Loads a model artifact. Must be implemented by subclasses."""
        raise NotImplementedError("Subclasses must implement _load_model.")

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        """Runs prediction. Must be implemented by subclasses."""
        raise NotImplementedError("Subclasses must implement _predict.")

    def _save_to_excel(self, dataframe: pd.DataFrame, predictions: pd.Series, output_dir: Path) -> dict:
        """Saves the input data with predictions to an Excel file."""
        report_df = dataframe.copy()
        report_df['predicted_y'] = predictions
        
        file_name = f"prediction_report.xlsx"
        output_path = output_dir / file_name
        report_df.to_excel(output_path, index=False)
        
        return { "report_file": file_name }

    def _format_output(self, predictions: pd.Series, excel_report: dict, input_dataframe: pd.DataFrame = None) -> dict:
        """Formats the final JSON output."""
        # 转换预测结果为列表
        predictions_list = predictions.tolist() if hasattr(predictions, 'tolist') else list(predictions)

        # 准备输入数据（如果提供）
        input_data = []
        if input_dataframe is not None:
            input_data = input_dataframe.to_dict('records')  # 转换为字典列表

        return {
            "predictions": predictions_list,  # 预测结果数组
            "input_data": input_data,  # 输入数据数组
            "statistics": {
                "message": f"Successfully made {len(predictions)} predictions.",
                "prediction_summary": predictions.describe().to_dict(),
                "task_type": "regression"  # 任务类型
            },
            "excel_report": excel_report
        }