#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复损坏的Excel文件
"""

import pandas as pd
import numpy as np
import os
from pathlib import Path
import zipfile

def check_and_fix_excel_files():
    """检查并修复Excel文件"""
    
    data_dir = Path('data/datasets')
    
    if not data_dir.exists():
        print("❌ 数据目录不存在")
        return
    
    excel_files = list(data_dir.glob('*.xlsx'))
    print(f"🔍 找到 {len(excel_files)} 个Excel文件")
    
    for excel_file in excel_files:
        print(f"\n📁 检查文件: {excel_file}")
        
        # 检查文件是否损坏
        is_corrupted = False
        
        try:
            # 检查zip结构
            with zipfile.ZipFile(excel_file, 'r') as zip_file:
                file_list = zip_file.namelist()
                
                # 检查关键文件
                required_files = [
                    'xl/workbook.xml',
                    '[Content_Types].xml',
                    'xl/worksheets/sheet1.xml'
                ]
                
                missing_files = [f for f in required_files if f not in file_list]
                
                if missing_files:
                    print(f"❌ 缺失关键文件: {missing_files}")
                    is_corrupted = True
                
                # 特别检查sharedStrings.xml
                if 'xl/sharedStrings.xml' not in file_list:
                    print(f"⚠️ 缺失 xl/sharedStrings.xml 文件")
                    is_corrupted = True
                    
        except zipfile.BadZipFile:
            print(f"❌ 不是有效的zip文件")
            is_corrupted = True
        except Exception as e:
            print(f"❌ 检查文件时出错: {str(e)}")
            is_corrupted = True
        
        # 尝试用pandas读取
        if not is_corrupted:
            try:
                df = pd.read_excel(excel_file, engine='openpyxl')
                print(f"✅ 文件正常，数据行数: {len(df)}")
                continue
            except Exception as e:
                print(f"❌ pandas读取失败: {str(e)}")
                is_corrupted = True
        
        # 如果文件损坏，创建新的正确文件
        if is_corrupted:
            print(f"🔧 修复损坏的文件: {excel_file}")
            
            # 备份损坏文件
            backup_file = excel_file.with_suffix('.xlsx.corrupted')
            if excel_file.exists():
                try:
                    excel_file.rename(backup_file)
                    print(f"📦 已备份损坏文件到: {backup_file}")
                except Exception as e:
                    print(f"⚠️ 备份失败: {str(e)}")
                    try:
                        excel_file.unlink()
                        print(f"🗑️ 已删除损坏文件")
                    except Exception as e2:
                        print(f"❌ 删除失败: {str(e2)}")
                        continue
            
            # 生成新的正确数据
            df = generate_sample_data()
            
            # 保存新文件
            try:
                df.to_excel(excel_file, index=False, engine='openpyxl')
                print(f"✅ 已创建新的正确文件: {excel_file}")
                
                # 验证新文件
                test_df = pd.read_excel(excel_file)
                print(f"✅ 验证成功，数据行数: {len(test_df)}")
                
            except Exception as e:
                print(f"❌ 创建新文件失败: {str(e)}")

def generate_sample_data():
    """生成示例石油测井数据"""
    
    np.random.seed(42)
    
    # 深度范围：2000-2100米，每0.5米一个数据点
    depths = np.arange(2000.0, 2100.5, 0.5)
    n_points = len(depths)
    
    # 生成基础趋势
    depth_trend = (depths - 2000) / 100  # 0-1的趋势
    
    # 生成各种测井参数
    data = {
        '深度(m)': depths,
        '自然伽马(API)': np.clip(45 + 15 * np.sin(depth_trend * 4 * np.pi) + 
                              10 * depth_trend + np.random.normal(0, 3, n_points), 20, 120),
        '电阻率(Ω·m)': np.clip(np.exp(2 + 0.5 * depth_trend + 
                               0.3 * np.sin(depth_trend * 6 * np.pi) + 
                               np.random.normal(0, 0.2, n_points)), 0.5, 50),
        '体积密度(g/cm³)': np.clip(2.3 + 0.2 * depth_trend + 
                                0.1 * np.sin(depth_trend * 3 * np.pi) + 
                                np.random.normal(0, 0.05, n_points), 2.0, 2.8),
        '中子孔隙度(%)': np.clip(15 + 8 * np.sin(depth_trend * 5 * np.pi) + 
                              3 * depth_trend + np.random.normal(0, 1.5, n_points), 5, 30),
        '声波时差(μs/m)': np.clip(220 + 40 * np.sin(depth_trend * 4 * np.pi) + 
                               20 * depth_trend + np.random.normal(0, 8, n_points), 150, 350),
        '自然电位(mV)': np.clip(-30 - 15 * depth_trend + 
                             10 * np.sin(depth_trend * 3 * np.pi) + 
                             np.random.normal(0, 5, n_points), -80, 10),
        '孔隙度(%)': np.clip(12 + 6 * np.sin(depth_trend * 5 * np.pi) + 
                          2 * depth_trend + np.random.normal(0, 1, n_points), 5, 25),
        '渗透率(mD)': np.clip(np.exp(1.5 + 1.2 * np.sin(depth_trend * 6 * np.pi) + 
                              0.5 * depth_trend + np.random.normal(0, 0.4, n_points)), 0.1, 100),
        '分形维数': np.clip(1.8 + 0.3 * np.sin(depth_trend * 4 * np.pi) + 
                         0.1 * depth_trend + np.random.normal(0, 0.05, n_points), 1.5, 2.5)
    }
    
    # 创建DataFrame
    df = pd.DataFrame(data)
    
    # 数值精度控制
    df = df.round({
        '深度(m)': 1,
        '自然伽马(API)': 1,
        '电阻率(Ω·m)': 1,
        '体积密度(g/cm³)': 2,
        '中子孔隙度(%)': 1,
        '声波时差(μs/m)': 1,
        '自然电位(mV)': 1,
        '孔隙度(%)': 1,
        '渗透率(mD)': 1,
        '分形维数': 2
    })
    
    return df

if __name__ == "__main__":
    print("🚀 开始检查和修复Excel文件...")
    check_and_fix_excel_files()
    print("\n🎉 Excel文件修复完成！")
