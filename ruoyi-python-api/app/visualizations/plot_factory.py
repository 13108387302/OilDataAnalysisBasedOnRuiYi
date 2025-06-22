# app/visualizations/plot_factory.py
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
from pathlib import Path
from sklearn.metrics import confusion_matrix, roc_curve, auc

def plot_well_log(dataframe, depth_col, data_col, output_dir: Path, title="Well Log Plot", file_name="well_log.png"):
    """
    Generates and saves a well log plot.

    Args:
        dataframe: Pandas DataFrame containing the data.
        depth_col: The name of the depth column.
        data_col: The name of the data column to plot.
        output_dir: The directory to save the plot file.
        title: The title of the plot.
        file_name: The name of the output plot file.

    Returns:
        The file name of the generated plot.
    """
    sns.set_theme(style="whitegrid")
    
    fig, ax = plt.subplots(figsize=(8, 12))
    
    sns.lineplot(x=data_col, y=depth_col, data=dataframe, ax=ax)
    
    ax.set_title(title, fontsize=16)
    ax.set_xlabel(data_col, fontsize=12)
    ax.set_ylabel("Depth", fontsize=12)
    
    # Crucial for well logs: reverse the Y-axis
    ax.invert_yaxis()
    
    plt.tight_layout()
    
    output_path = output_dir / file_name
    plt.savefig(output_path)
    plt.close(fig)  # Close the figure to free up memory

    # 返回Web可访问的路径
    parts = output_path.parts
    try:
        petrol_index = parts.index('petrol')
        web_path = Path(*parts[petrol_index:]).as_posix()
        return f"/profile/{web_path}"
    except ValueError:
        return str(output_path)

def plot_regression(dataframe, x_col, y_col, y_pred, output_dir: Path, title="Regression Plot", file_name="regression_plot.png", x_pred=None):
    """
    Generates and saves a regression plot with actual vs. predicted values.

    Args:
        dataframe: Pandas DataFrame containing the original data.
        x_col: The name of the independent variable column.
        y_col: The name of the dependent variable column.
        y_pred: The predicted values from the regression model.
        output_dir: The directory to save the plot file.
        title: The title of the plot.
        file_name: The name of the output plot file.
        x_pred: The x-values corresponding to y_pred. If None, assumes y_pred corresponds to dataframe[x_col].

    Returns:
        The file name of the generated plot.
    """
    sns.set_theme(style="whitegrid")
    
    fig, ax = plt.subplots(figsize=(10, 8))
    
    # Plot original data points
    sns.scatterplot(x=x_col, y=y_col, data=dataframe, ax=ax, label="Actual Data", color="blue", alpha=0.6)
    
    # Plot the regression line
    if x_pred is not None:
        ax.plot(x_pred, y_pred, color='red', linewidth=2, label="Regression Line")
    else:
        ax.plot(dataframe[x_col], y_pred, color='red', linewidth=2, label="Regression Line")
    
    ax.set_title(title, fontsize=16)
    ax.set_xlabel(x_col, fontsize=12)
    ax.set_ylabel(y_col, fontsize=12)
    ax.legend()
    
    plt.tight_layout()
    
    output_path = output_dir / file_name
    plt.savefig(output_path)
    plt.close(fig)

    # 返回Web可访问的路径
    parts = output_path.parts
    try:
        petrol_index = parts.index('petrol')
        web_path = Path(*parts[petrol_index:]).as_posix()
        return f"/profile/{web_path}"
    except ValueError:
        return str(output_path)

def plot_scatter(dataframe, x_col, y_col, output_dir: Path, title="Scatter Plot", file_name="scatter_plot.png"):
    """
    Generates and saves a simple scatter plot.

    Args:
        dataframe: Pandas DataFrame containing the data.
        x_col: The name of the independent variable column.
        y_col: The name of the dependent variable column.
        output_dir: The directory to save the plot file.
        title: The title of the plot.
        file_name: The name of the output plot file.

    Returns:
        The file name of the generated plot.
    """
    sns.set_theme(style="whitegrid")
    
    fig, ax = plt.subplots(figsize=(10, 8))
    
    # Plot data points
    sns.scatterplot(x=x_col, y=y_col, data=dataframe, ax=ax, alpha=0.7)
    
    ax.set_title(title, fontsize=16)
    ax.set_xlabel(x_col, fontsize=12)
    ax.set_ylabel(y_col, fontsize=12)
    
    plt.tight_layout()
    
    output_path = output_dir / file_name
    plt.savefig(output_path)
    plt.close(fig)

    # 返回Web可访问的路径
    parts = output_path.parts
    try:
        petrol_index = parts.index('petrol')
        web_path = Path(*parts[petrol_index:]).as_posix()
        return f"/profile/{web_path}"
    except ValueError:
        return str(output_path)

def plot_confusion_matrix(y_true, y_pred, output_dir: Path, title="Confusion Matrix", file_name="confusion_matrix.png", class_names=None):
    """
    Generates and saves a confusion matrix plot.

    Args:
        y_true: True labels.
        y_pred: Predicted labels.
        output_dir: The directory to save the plot file.
        title: The title of the plot.
        file_name: The name of the output plot file.
        class_names: List of class names for labeling.

    Returns:
        The file name of the generated plot.
    """
    cm = confusion_matrix(y_true, y_pred)

    plt.figure(figsize=(8, 6))
    sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
                xticklabels=class_names, yticklabels=class_names)
    plt.title(title, fontsize=16)
    plt.xlabel('Predicted Label', fontsize=12)
    plt.ylabel('True Label', fontsize=12)
    plt.tight_layout()

    output_path = output_dir / file_name
    plt.savefig(output_path)
    plt.close()

    return file_name

def plot_roc_curve(y_true, y_pred_proba=None, y_prob=None, output_dir=None, title="ROC Curve", file_name="roc_curve.png", class_names=None):
    """
    Generates and saves a ROC curve plot.

    Args:
        y_true: True binary labels.
        y_pred_proba: Predicted probabilities for the positive class (deprecated, use y_prob).
        y_prob: Predicted probabilities for all classes.
        output_dir: The directory to save the plot file.
        title: The title of the plot.
        file_name: The name of the output plot file.
        class_names: List of class names (optional).

    Returns:
        The file name of the generated plot.
    """
    # Handle backward compatibility
    if y_prob is None and y_pred_proba is not None:
        y_prob = y_pred_proba

    if y_prob is None:
        raise ValueError("Either y_pred_proba or y_prob must be provided")

    plt.figure(figsize=(8, 6))

    # Handle multi-class ROC curve
    if y_prob.ndim > 1 and y_prob.shape[1] > 2:
        # Multi-class case: plot ROC curve for each class
        from sklearn.preprocessing import label_binarize
        from sklearn.metrics import roc_curve, auc

        n_classes = y_prob.shape[1]
        y_true_bin = label_binarize(y_true, classes=range(n_classes))

        for i in range(n_classes):
            fpr, tpr, _ = roc_curve(y_true_bin[:, i], y_prob[:, i])
            roc_auc = auc(fpr, tpr)
            class_name = class_names[i] if class_names and i < len(class_names) else f'Class {i}'
            plt.plot(fpr, tpr, lw=2, label=f'{class_name} (AUC = {roc_auc:.2f})')
    else:
        # Binary classification case
        if y_prob.ndim > 1:
            y_prob_positive = y_prob[:, 1]  # Use positive class probabilities
        else:
            y_prob_positive = y_prob

        fpr, tpr, _ = roc_curve(y_true, y_prob_positive)
        roc_auc = auc(fpr, tpr)
        plt.plot(fpr, tpr, color='darkorange', lw=2, label=f'ROC curve (AUC = {roc_auc:.2f})')

    plt.plot([0, 1], [0, 1], color='navy', lw=2, linestyle='--')
    plt.xlim([0.0, 1.0])
    plt.ylim([0.0, 1.05])
    plt.xlabel('False Positive Rate', fontsize=12)
    plt.ylabel('True Positive Rate', fontsize=12)
    plt.title(title, fontsize=16)
    plt.legend(loc="lower right")
    plt.tight_layout()

    output_path = output_dir / file_name
    plt.savefig(output_path)
    plt.close()

    return file_name

def plot_clusters(dataframe, x_col, y_col, cluster_col, output_dir: Path, title="Cluster Plot", file_name="clusters.png"):
    """
    Generates and saves a cluster visualization plot.

    Args:
        dataframe: Pandas DataFrame containing the data.
        x_col: The name of the x-axis column.
        y_col: The name of the y-axis column.
        cluster_col: The name of the cluster label column.
        output_dir: The directory to save the plot file.
        title: The title of the plot.
        file_name: The name of the output plot file.

    Returns:
        The file name of the generated plot.
    """
    plt.figure(figsize=(10, 8))

    # Get unique clusters
    unique_clusters = dataframe[cluster_col].unique()
    colors = plt.cm.Set1(np.linspace(0, 1, len(unique_clusters)))

    for i, cluster in enumerate(unique_clusters):
        cluster_data = dataframe[dataframe[cluster_col] == cluster]
        plt.scatter(cluster_data[x_col], cluster_data[y_col],
                   c=[colors[i]], label=f'Cluster {cluster}', alpha=0.7)

    plt.title(title, fontsize=16)
    plt.xlabel(x_col, fontsize=12)
    plt.ylabel(y_col, fontsize=12)
    plt.legend()
    plt.tight_layout()

    output_path = output_dir / file_name
    plt.savefig(output_path)
    plt.close()

    # 返回Web可访问的路径
    parts = output_path.parts
    try:
        petrol_index = parts.index('petrol')
        web_path = Path(*parts[petrol_index:]).as_posix()
        return f"/profile/{web_path}"
    except ValueError:
        return str(output_path)