# app/algorithms/base_predictor.py
import pandas as pd
from pathlib import Path

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
        dataframe = self._load_data()
        predictions = self._predict(dataframe, model_artifact)
        
        excel_report = self._save_to_excel(dataframe, predictions, self.output_dir)
        
        return self._format_output(predictions, excel_report)

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

    def _format_output(self, predictions: pd.Series, excel_report: dict) -> dict:
        """Formats the final JSON output."""
        return {
            "statistics": {
                "message": f"Successfully made {len(predictions)} predictions.",
                "prediction_summary": predictions.describe().to_dict()
            },
            "excel_report": excel_report
        } 