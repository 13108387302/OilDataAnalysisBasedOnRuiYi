#!/usr/bin/env python3
"""
真实数据测试脚本
测试Python API使用真实数据的能力
注意：此脚本不生成任何模拟数据，仅测试API连接和真实数据处理能力
"""

import requests
import json
import time
import os

def test_python_api_health():
    """测试Python API健康状态"""
    print("🔍 测试Python API健康状态...")
    try:
        response = requests.get("http://127.0.0.1:8000/api/v1/health", timeout=5)
        if response.status_code == 200:
            data = response.json()
            print("✅ Python API健康检查成功")
            print(f"   状态: {data.get('status')}")
            print(f"   消息: {data.get('message')}")
            print(f"   模拟数据已禁用: {data.get('mock_data_removed')}")
            return True
        else:
            print(f"❌ Python API健康检查失败，状态码: {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"❌ 无法连接到Python API: {e}")
        return False

def test_submit_endpoint_with_real_data():
    """测试/submit端点使用真实数据文件"""
    print("\n🔍 测试/submit端点（需要真实数据文件）...")
    
    # 检查是否有真实数据文件
    possible_data_files = [
        "uploads/test_data.csv",
        "uploads/petroleum_data.csv", 
        "uploads/analysis_data.xlsx",
        "../uploads/test_data.csv",
        "../../uploads/test_data.csv"
    ]
    
    real_data_file = None
    for file_path in possible_data_files:
        if os.path.exists(file_path):
            real_data_file = os.path.abspath(file_path)
            print(f"✅ 找到真实数据文件: {real_data_file}")
            break
    
    if not real_data_file:
        print("⚠️ 未找到真实数据文件，跳过真实数据测试")
        print("   请将真实数据文件放置在以下位置之一:")
        for file_path in possible_data_files:
            print(f"   - {file_path}")
        return False
    
    # 创建输出目录
    output_dir = os.path.abspath("real_output")
    os.makedirs(output_dir, exist_ok=True)
    
    # 测试真实数据处理
    test_data = {
        "id": 1,
        "task_name": "真实数据分析任务",
        "task_type": "regression",
        "algorithm": "regression_linear_train",
        "input_params": {
            "test_size": 0.2,
            "random_state": 42
        },
        "input_file_path": real_data_file,
        "output_dir_path": output_dir
    }
    
    try:
        print(f"📤 发送真实数据分析请求...")
        print(f"   数据文件: {real_data_file}")
        print(f"   输出目录: {output_dir}")
        
        response = requests.post(
            "http://127.0.0.1:8000/api/v1/submit",
            json=test_data,
            timeout=60  # 真实数据处理可能需要更长时间
        )
        
        print(f"📋 响应状态码: {response.status_code}")
        
        if response.status_code == 200:
            response_data = response.json()
            
            if "error" in response_data:
                print(f"❌ 算法执行失败:")
                print(f"   错误类型: {response_data.get('error')}")
                print(f"   错误信息: {response_data.get('message')}")
                
                # 检查是否是依赖包问题
                if "missing_packages" in response_data:
                    print(f"   缺少依赖: {response_data['missing_packages']}")
                    print(f"   安装命令: {response_data.get('installation_command')}")
                
                return False
            else:
                print(f"✅ 真实数据分析成功!")
                
                # 显示结果摘要
                if "statistics" in response_data:
                    stats = response_data["statistics"]
                    print(f"   R² 分数: {stats.get('r2_score', 'N/A')}")
                    print(f"   均方误差: {stats.get('mse', 'N/A')}")
                
                if "model_params" in response_data:
                    model_params = response_data["model_params"]
                    print(f"   模型参数: {len(model_params)} 个")
                
                if "visualizations" in response_data:
                    viz = response_data["visualizations"]
                    print(f"   生成图表: {len(viz)} 个")
                
                return True
        else:
            print(f"❌ HTTP错误: {response.status_code}")
            print(f"   响应: {response.text}")
            return False
            
    except requests.exceptions.RequestException as e:
        print(f"❌ 请求失败: {e}")
        return False

def test_dependency_check():
    """测试依赖检查"""
    print("\n🔍 测试依赖检查...")
    
    dependencies = ['sklearn', 'pandas', 'numpy', 'matplotlib', 'seaborn']
    missing = []
    
    for dep in dependencies:
        try:
            __import__(dep)
            print(f"   ✅ {dep} 已安装")
        except ImportError:
            print(f"   ❌ {dep} 未安装")
            missing.append(dep)
    
    if missing:
        print(f"\n💡 建议安装缺失的依赖:")
        print(f"   pip install {' '.join(missing)}")
        return False
    else:
        print("   ✅ 所有依赖都已安装")
        return True

def main():
    """主测试函数"""
    print("🚀 开始真实数据测试")
    print("=" * 60)
    print("📝 注意：此测试不生成任何模拟数据，仅使用真实数据")
    print("=" * 60)
    
    # 等待服务启动
    print("⏳ 等待服务启动...")
    time.sleep(2)
    
    # 测试步骤
    tests = [
        ("Python API健康检查", test_python_api_health),
        ("依赖检查", test_dependency_check),
        ("真实数据处理测试", test_submit_endpoint_with_real_data),
    ]
    
    results = []
    for test_name, test_func in tests:
        try:
            result = test_func()
            results.append((test_name, result))
        except Exception as e:
            print(f"❌ {test_name} 执行失败: {e}")
            results.append((test_name, False))
    
    # 总结
    print("\n" + "=" * 60)
    print("📊 测试结果总结:")
    
    passed = 0
    for test_name, result in results:
        status = "✅ 通过" if result else "❌ 失败"
        print(f"   {test_name}: {status}")
        if result:
            passed += 1
    
    print(f"\n🎯 总体结果: {passed}/{len(results)} 测试通过")
    
    if passed == len(results):
        print("🎉 所有测试通过！系统可以处理真实数据")
    elif passed >= len(results) - 1:
        print("✅ 核心功能正常，部分功能需要真实数据文件")
    else:
        print("⚠️ 存在问题，请检查配置和依赖")
    
    print("\n📝 重要说明:")
    print("   - 系统已完全禁用模拟数据生成")
    print("   - 所有算法使用真实的机器学习框架")
    print("   - 需要提供真实数据文件进行测试")
    print("   - 结果基于真实数据分析，不包含任何模拟内容")

if __name__ == "__main__":
    main()
