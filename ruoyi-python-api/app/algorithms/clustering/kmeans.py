# app/algorithms/clustering/kmeans.py
import pandas as pd
import numpy as np
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import silhouette_score, calinski_harabasz_score, davies_bouldin_score
from pathlib import Path
from joblib import dump, load

from app.algorithms.base_algorithm import BaseAlgorithm
from app.visualizations.plot_factory import plot_clusters
from app.algorithms.base_predictor import BasePredictorAlgorithm

class Trainer(BaseAlgorithm):
    """
    Performs K-Means clustering on given data.
    """
    def validate_params(self):
        """
        Validates parameters for K-Means Clustering.
        """
        if 'feature_columns' not in self.params:
            raise ValueError("Parameter 'feature_columns' is required.")
        if 'n_clusters' not in self.params:
            raise ValueError("Parameter 'n_clusters' (K value) is required.")

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Trains a K-Means model and returns evaluation metrics.
        """
        feature_cols = self.params['feature_columns']
        if isinstance(feature_cols, str):
            feature_cols = [feature_cols]

        n_clusters = int(self.params['n_clusters'])
        random_state = self.params.get('random_state', 42)

        # Prepare data
        X = dataframe[feature_cols].dropna()

        # Scale features
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)

        # Train model
        model = KMeans(
            n_clusters=n_clusters,
            random_state=random_state,
            n_init=10
        )
        model.fit(X_scaled)
        labels = model.labels_

        # Calculate metrics if there is more than 1 cluster
        metrics = { "inertia": model.inertia_ }
        if n_clusters > 1 and len(set(labels)) > 1:
            metrics["silhouette_score"] = silhouette_score(X_scaled, labels)
            metrics["calinski_harabasz_score"] = calinski_harabasz_score(X_scaled, labels)
            metrics["davies_bouldin_score"] = davies_bouldin_score(X_scaled, labels)

        # Add cluster labels to the original dataframe
        result_df = X.copy()
        result_df['cluster'] = labels

        return {
            "statistics": metrics,
            "model_params": {
                "n_clusters": int(n_clusters),
                "feature_columns": feature_cols
            },
            "dataframe_with_clusters": result_df,
            "model_object": model,
            "scaler": scaler,
            # 标准数据字段
            "predictions": labels,
            "cluster_labels": labels,
            "cluster_centers": model.cluster_centers_,
            "feature_values": X_scaled,
            "cluster_sizes": {str(i): int(count) for i, count in enumerate(np.bincount(labels))},
            "input_sample": dataframe[feature_cols].head(100).to_dict('records')
        }

    def _save_to_excel(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the dataframe with cluster labels to an Excel file.
        """
        df_with_clusters = computed_data.get('dataframe_with_clusters')
        if df_with_clusters is None:
            return {}

        file_name = "kmeans_clustered_data.xlsx"
        output_path = output_dir / file_name
        df_with_clusters.to_excel(output_path, index=False)
        
        return { "clustered_data_file": file_name }

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """
        Saves the trained K-Means model and scaler.
        """
        model = computed_data.get('model_object')
        scaler = computed_data.get('scaler')
        feature_cols = computed_data.get('feature_columns')
        if model is None or scaler is None:
            return {}
            
        model_package = {
            'model': model,
            'scaler': scaler,
            'feature_columns': feature_cols # Save feature columns for prediction
        }
            
        file_name = "kmeans_model.joblib"
        output_path = output_dir / file_name
        dump(model_package, output_path)
        
        return { "model_file": file_name }

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Generates a scatter plot of the clusters.
        """
        feature_cols = computed_data.get('feature_columns')
        if not feature_cols or len(feature_cols) < 2:
            # Cannot plot clusters for single-dimensional data
            return {}

        x_col = feature_cols[0]
        y_col = feature_cols[1]
        
        plot_df = computed_data['dataframe_with_clusters']

        # The centroids are in scaled space. Inverse transform them back to original space.
        centroids_scaled = computed_data['model_object'].cluster_centers_
        scaler = computed_data['scaler']
        unscaled_centroids = scaler.inverse_transform(centroids_scaled)

        # For plotting, we only need the coordinates for the two plotted dimensions
        x_col_index = feature_cols.index(x_col)
        y_col_index = feature_cols.index(y_col)
        centroids_2d = unscaled_centroids[:, [x_col_index, y_col_index]]

        plot_filename = plot_clusters(
            dataframe=plot_df,
            x_col=x_col,
            y_col=y_col,
            cluster_col='cluster',
            output_dir=output_dir,
            title=f"K-Means Clustering: {y_col} vs {x_col}",
            file_name=f"kmeans_cluster_plot.png"
        )

        return { "cluster_plot": plot_filename }

class Predictor(BasePredictorAlgorithm):
    """
    Predicts cluster assignments using a pre-trained K-Means model.
    """
    def _load_model(self, model_path: Path):
        """Loads the saved K-Means model package."""
        return load(model_path)

    def _predict(self, dataframe: pd.DataFrame, model_artifact) -> pd.Series:
        """Runs prediction on new data."""
        model = model_artifact['model']
        scaler = model_artifact['scaler']
        feature_cols = model_artifact['feature_columns']

        # Ensure all required feature columns are present
        missing_cols = set(feature_cols) - set(dataframe.columns)
        if missing_cols:
            raise ValueError(f"Missing required columns for prediction: {list(missing_cols)}")
            
        X_new = dataframe[feature_cols]
        X_scaled = scaler.transform(X_new)
        
        predictions = model.predict(X_scaled)
        return pd.Series(predictions, name='predicted_cluster') 