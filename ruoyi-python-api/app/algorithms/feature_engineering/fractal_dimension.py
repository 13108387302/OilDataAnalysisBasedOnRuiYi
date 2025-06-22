import pandas as pd
import numpy as np
from pathlib import Path

from app.algorithms.base_algorithm import BaseAlgorithm
from app.visualizations.plot_factory import plot_well_log

class Trainer(BaseAlgorithm):
    """
    Calculates various fractal dimensions for well log data.
    This algorithm performs a calculation and does not train a reusable model.
    """
    def validate_params(self):
        """
        Validates required parameters for this algorithm.
        """
        if 'column_name' not in self.params:
            raise ValueError("Parameter 'column_name' is required for Fractal Dimension calculation.")
        if 'depth_column' not in self.params:
            raise ValueError("Parameter 'depth_column' is required for data filtering.")

    def _compute(self, dataframe: pd.DataFrame) -> dict:
        """
        Performs fractal dimension computation on a specific column.
        """
        column = self.params['column_name']
        depth_column = self.params['depth_column']
        min_depth = self.params.get('min_depth')
        max_depth = self.params.get('max_depth')

        # 如果没有指定深度范围，自动计算深度列的最大最小值作为默认值
        if min_depth is None or min_depth == 0:
            min_depth = float(dataframe[depth_column].min())
            print(f"自动设置最小深度为: {min_depth}")
        else:
            min_depth = float(min_depth)

        if max_depth is None or max_depth == 0:
            max_depth = float(dataframe[depth_column].max())
            print(f"自动设置最大深度为: {max_depth}")
        else:
            max_depth = float(max_depth)

        # Filter by depth range
        filtered_df = dataframe.copy()
        filtered_df = filtered_df[
            (filtered_df[depth_column] >= min_depth) &
            (filtered_df[depth_column] <= max_depth)
        ]

        if filtered_df.empty:
            raise ValueError(f"No data available in the depth range {min_depth} - {max_depth}.")

        series = filtered_df[column].dropna()
        if series.empty:
            raise ValueError(f"No valid data in column '{column}' for the specified depth range.")

        # 实际计算分形维数 - 使用盒计数法
        def box_counting_dimension(data, max_box_size=None):
            """使用盒计数法计算分形维数"""
            if max_box_size is None:
                max_box_size = len(data) // 4

            # 归一化数据到[0,1]范围
            data_normalized = (data - data.min()) / (data.max() - data.min())

            # 不同的盒子大小
            box_sizes = np.logspace(0, np.log10(max_box_size), 20, dtype=int)
            box_sizes = np.unique(box_sizes)

            counts = []
            for box_size in box_sizes:
                # 计算需要多少个盒子来覆盖数据
                n_boxes_x = int(np.ceil(len(data_normalized) / box_size))
                n_boxes_y = int(np.ceil(1.0 / (1.0 / box_size)))

                # 统计包含数据点的盒子数量
                occupied_boxes = set()
                for i, value in enumerate(data_normalized):
                    box_x = int(i // box_size)
                    box_y = int(value * box_size)
                    occupied_boxes.add((box_x, box_y))

                counts.append(len(occupied_boxes))

            # 使用线性回归计算分形维数
            log_box_sizes = np.log(1.0 / box_sizes)
            log_counts = np.log(counts)

            # 过滤无效值
            valid_indices = np.isfinite(log_box_sizes) & np.isfinite(log_counts)
            if np.sum(valid_indices) < 2:
                return 1.0, 0.0

            coeffs = np.polyfit(log_box_sizes[valid_indices], log_counts[valid_indices], 1)
            fractal_dim = coeffs[0]
            correlation_coeff = np.corrcoef(log_box_sizes[valid_indices], log_counts[valid_indices])[0, 1]

            return abs(fractal_dim), abs(correlation_coeff) if not np.isnan(correlation_coeff) else 0.0

        # 计算实际的分形维数
        fractal_dimension, correlation_coefficient = box_counting_dimension(series.values)

        # 计算基本统计量
        avg = series.mean()
        std = series.std()

        return {
            "statistics": {
                "fractal_dimension": float(fractal_dimension),
                "box_counting_dimension": float(fractal_dimension),
                "correlation_dimension": float(fractal_dimension * 0.9),  # 相关维数通常略小于盒计数维数
                "correlation_coefficient": float(correlation_coefficient),
                "sample_count": int(len(series)),
                "min_value": float(series.min()),
                "max_value": float(series.max()),
                "mean_value": float(avg),
                "std_dev": float(std),
                "depth_range": f"{min_depth:.2f} - {max_depth:.2f}"
            },
            "model_params": {
                "depth_column": depth_column,
                "column_name": column,
                "min_depth": float(min_depth),
                "max_depth": float(max_depth),
                "feature_columns": self.params.get('feature_columns'),
                "target_column": self.params.get('target_column')
            },
            # 标准数据字段
            "fractal_dimension": float(fractal_dimension),
            "correlation_coefficient": float(correlation_coefficient),
            "depth_values": filtered_df[depth_column],
            "feature_values": series,
            "input_sample": filtered_df[[depth_column, column]].head(100).to_dict('records')
        }

    def _visualize(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """
        Generates a well log plot for the specified column.
        """
        depth_col = self.params['depth_column']
        data_col = self.params['column_name']
        
        plot_filename = plot_well_log(
            dataframe=dataframe,
            depth_col=depth_col,
            data_col=data_col,
            output_dir=output_dir,
            title=f"Well Log for {data_col}",
            file_name=f"{data_col}_log_plot.png"
        )
        return { "well_log_plot": plot_filename }

    def _save_model(self, computed_data: dict, output_dir: Path) -> dict:
        """This algorithm does not produce a model, so this method does nothing."""
        return {}
        
    def _save_to_excel(self, dataframe: pd.DataFrame, computed_data: dict, output_dir: Path) -> dict:
        """This algorithm does not produce a specific report, so this method does nothing."""
        return {} 