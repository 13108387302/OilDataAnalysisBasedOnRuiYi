# app/algorithms/classification/logistic.py
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, roc_auc_score, confusion_matrix
from sklearn.preprocessing import StandardScaler, LabelEncoder
from pathlib import Path
from joblib import dump, load
import numpy as np

from app.algorithms.base_algorithm import BaseAlgorithm
from app.visualizations.plot_factory import plot_confusion_matrix, plot_roc_curve
from app.algorithms.base_predictor import BasePredictorAlgorithm

class Trainer(BaseAlgorithm):
    """
    Performs Logistic Regression for classification.
    """
    def validate_params(self):
        """
        Validates parameters for Logistic Regression.
        """
        if 'feature_columns' not in self.params:
            raise ValueError("Parameter 'feature_columns' is required.")
        if 'target_column' not in self.params:
            raise ValueError("Parameter 'target_column' is required.")

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Trains a Logistic Regression model and returns evaluation metrics.
        """
        feature_cols = self.params['feature_columns']
        if isinstance(feature_cols, str):
            feature_cols = [feature_cols]

        target_col = self.params['target_column']
        test_size = self.params.get('test_size', 0.2)
        random_state = self.params.get('random_state', 42)
        
        penalty = self.params.get('penalty', 'l2')
        C = float(self.params.get('C', 1.0))
        solver = 'liblinear' if penalty in ['l1', 'l2'] else 'saga'

        X = dataframe[feature_cols]
        y = dataframe[target_col]

        le = LabelEncoder()
        y_encoded = le.fit_transform(y)
        class_names = le.classes_.tolist()

        X_train, X_test, y_train, y_test = train_test_split(
            X, y_encoded, test_size=test_size, random_state=random_state, stratify=y_encoded
        )

        scaler = StandardScaler()
        X_train_scaled = scaler.fit_transform(X_train)
        X_test_scaled = scaler.transform(X_test)

        model = LogisticRegression(
            penalty=penalty, C=C, random_state=random_state,
            solver=solver, max_iter=1000, n_jobs=-1
        )
        model.fit(X_train_scaled, y_train)

        y_pred = model.predict(X_test_scaled)
        y_prob = model.predict_proba(X_test_scaled)
        
        accuracy = accuracy_score(y_test, y_pred)
        precision = precision_score(y_test, y_pred, average='weighted', zero_division=0)
        recall = recall_score(y_test, y_pred, average='weighted', zero_division=0)
        f1 = f1_score(y_test, y_pred, average='weighted', zero_division=0)
        conf_matrix = confusion_matrix(y_test, y_pred).tolist()
        
        try:
            # AUC requires probabilities of the positive class for binary, or shape (n_samples, n_classes) for multi-class
            if len(class_names) == 2:
                auc = roc_auc_score(y_test, y_prob[:, 1])
            else:
                auc = roc_auc_score(y_test, y_prob, multi_class='ovr', average='weighted')
        except ValueError:
            auc = "N/A (check data)"

        return {
            "statistics": {
                "accuracy": float(accuracy),
                "precision": float(precision),
                "recall": float(recall),
                "f1_score": float(f1),
                "roc_auc_score": float(auc) if isinstance(auc, (int, float)) else auc,
                "confusion_matrix": conf_matrix,
            },
            "model_params": {
                "penalty": penalty,
                "C": float(C),
                "feature_columns": feature_cols,
                "target_column": target_col
            },
            "model_object": model,
            "scaler": scaler,
            "label_encoder": le,
            "class_names": class_names,
            # 标准数据字段
            "predictions": y_pred,
            "actual_values": y_test,
            "probabilities": y_prob,
            "feature_values": X_test_scaled,
            "feature_importance": dict(zip(feature_cols, np.abs(model.coef_[0]))),  # 使用系数绝对值作为特征重要性
            "class_distribution": {str(cls): int(count) for cls, count in zip(*np.unique(y_test, return_counts=True))},
            "input_sample": dataframe[feature_cols + [target_col]].head(100).to_dict('records')
        }

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        model_package = {
            'model': computed_data.get('model_object'),
            'scaler': computed_data.get('scaler'),
            'label_encoder': computed_data.get('label_encoder'),
            'feature_columns': computed_data.get('feature_columns')
        }
        file_name = "logistic_classification_model.joblib"
        output_path = output_dir / file_name
        dump(model_package, output_path)
        return { "model_file": file_name }

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        visualizations = {}

        # 安全地获取数据，避免numpy数组的or操作歧义
        y_test = computed_data.get('actual_values')
        if y_test is None:
            y_test = computed_data.get('y_test')

        y_pred = computed_data.get('predictions')
        if y_pred is None:
            y_pred = computed_data.get('y_pred')

        class_names = computed_data.get('class_names')
        
        cm_filename = plot_confusion_matrix(
            y_true=y_test, y_pred=y_pred, class_names=class_names,
            output_dir=output_dir, file_name="logistic_confusion_matrix.png", title="Logistic Regression Confusion Matrix"
        )
        visualizations["confusion_matrix_plot"] = cm_filename

        y_prob = computed_data.get('probabilities')
        if y_prob is None:
            y_prob = computed_data.get('y_prob')
        if y_prob is not None and len(class_names) > 1:
            roc_filename = plot_roc_curve(
                y_true=y_test, y_prob=y_prob, class_names=class_names,
                output_dir=output_dir, file_name="logistic_roc_curve.png", title="Logistic Regression ROC Curve"
            )
            visualizations["roc_curve_plot"] = roc_filename
            
        return visualizations
    
    def _save_to_excel(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        # 使用标准字段名，安全地获取数据
        y_test = computed_data.get('actual_values')
        if y_test is None:
            y_test = computed_data.get('y_test')

        y_pred = computed_data.get('predictions')
        if y_pred is None:
            y_pred = computed_data.get('y_pred')

        label_encoder = computed_data.get('label_encoder')

        if y_test is None or y_pred is None or label_encoder is None:
            return {}

        report_df = pd.DataFrame({
            'y_true': label_encoder.inverse_transform(y_test),
            'y_predicted': label_encoder.inverse_transform(y_pred)
        })
        file_name = f"logistic_classification_report.xlsx"
        output_path = output_dir / file_name
        report_df.to_excel(output_path, index=False)
        return {"report_file": file_name}

class Predictor(BasePredictorAlgorithm):
    """
    Predicts using a pre-trained Logistic Regression model.
    """
    def _load_model(self, model_path: Path):
        return load(model_path)

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        model, scaler, le, feature_cols = model_artifact['model'], model_artifact['scaler'], model_artifact['label_encoder'], model_artifact['feature_columns']
        
        missing_cols = set(feature_cols) - set(dataframe.columns)
        if missing_cols:
            raise ValueError(f"Missing required columns for prediction: {list(missing_cols)}")

        X_new = dataframe[feature_cols]
        X_scaled = scaler.transform(X_new)
        
        predictions_encoded = model.predict(X_scaled)
        predictions_decoded = le.inverse_transform(predictions_encoded)
        
        return pd.Series(predictions_decoded, name='predicted_class')
