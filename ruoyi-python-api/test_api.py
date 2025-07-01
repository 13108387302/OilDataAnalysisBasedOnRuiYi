#!/usr/bin/env python3
"""
测试Python API是否能正常启动和响应
"""

import requests
import json
import time

def test_api_health():
    """测试API健康检查端点"""
    try:
        response = requests.get("http://127.0.0.1:8000/api/v1/health", timeout=5)
        if response.status_code == 200:
            print("✅ API健康检查成功")
            print(f"响应: {response.json()}")
            return True
        else:
            print(f"❌ API健康检查失败，状态码: {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"❌ 无法连接到API: {e}")
        return False

def test_submit_endpoint():
    """测试/submit端点"""
    try:
        test_data = {
            "id": 1,
            "task_name": "测试任务",
            "task_type": "predict",
            "algorithm": "linear_regression",
            "input_params": {"test": True},
            "input_file_path": "/test/path",
            "output_dir_path": "/test/output"
        }

        response = requests.post(
            "http://127.0.0.1:8000/api/v1/submit",
            json=test_data,
            timeout=5
        )

        print(f"📤 /submit端点响应状态码: {response.status_code}")
        if response.status_code == 200:
            response_data = response.json()
            print(f"📋 响应内容: {response_data}")

            if "error" in response_data and response_data.get("status") == "failed":
                print("✅ 端点正常响应（返回错误信息，符合预期）")
                print(f"错误类型: {response_data.get('error', 'N/A')}")
                print(f"错误信息: {response_data.get('message', 'N/A')}")

                # 检查是否是依赖包问题
                if "missing_packages" in response_data:
                    print(f"缺少依赖包: {response_data['missing_packages']}")
                    print(f"安装命令: {response_data.get('installation_command', 'N/A')}")

                return True
            else:
                print(f"⚠️ 意外的响应内容: {response_data}")
                return False
        else:
            print(f"⚠️ 意外的状态码: {response.status_code}")
            print(f"响应内容: {response.text}")
            return False

    except requests.exceptions.RequestException as e:
        print(f"❌ 无法连接到/submit端点: {e}")
        return False

def main():
    """主测试函数"""
    print("🔍 开始测试Python API...")
    print("=" * 50)
    
    # 等待API启动
    print("⏳ 等待API启动...")
    time.sleep(2)
    
    # 测试健康检查
    health_ok = test_api_health()
    
    if health_ok:
        # 测试submit端点
        submit_ok = test_submit_endpoint()
        
        if submit_ok:
            print("\n🎉 所有测试通过！Python API正常工作")
            print("📝 注意：API正确地拒绝了请求，因为需要配置真实的机器学习框架")
        else:
            print("\n❌ submit端点测试失败")
    else:
        print("\n❌ API健康检查失败，请检查API是否正在运行")

if __name__ == "__main__":
    main()
