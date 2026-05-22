import os
import csv
import json
import time
import base64
from ai_analyzer import ZhipuVisionAnalyzer, WhiteboardType   # 根据实际导入路径修改

def run_batch_test(analyzer, manifest_path=None, image_dir=None):
     # 获取当前脚本文件所在目录的绝对路径
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # 如果调用时未指定路径，则默认使用脚本目录下的文件/文件夹
    if manifest_path is None:
        manifest_path = os.path.join(script_dir, "test_manifest.csv")
    if image_dir is None:
        image_dir = os.path.join(script_dir, "test_samples")
        
    results = []
    with open(manifest_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            img_path = os.path.join(image_dir, row['filename'])
            if not os.path.exists(img_path):
                print(f"跳过 {img_path}，文件不存在")
                continue
            with open(img_path, "rb") as img_f:
                b64 = base64.b64encode(img_f.read()).decode()
            
            # 映射类型
            type_map = {"code": WhiteboardType.CODE, "math": WhiteboardType.MATH, "general": WhiteboardType.GENERAL}
            wb_type = type_map.get(row['type'], WhiteboardType.GENERAL)
            
            start = time.time()
            resp = analyzer.analyze_image(b64, whiteboard_type=wb_type, context="手写测试", strict_format=True)
            elapsed = time.time() - start
            
            valid_json = False
            try:
                data = json.loads(resp.content)
                valid_json = True
                confidence = data.get("confidence", 0)
            except:
                data = {}
                confidence = 0
            
            results.append({
                "filename": row['filename'],
                "difficulty": row['difficulty'],
                "expected_keywords": row['expected_keywords'],
                "success": resp.success,
                "valid_json": valid_json,
                "confidence": confidence,
                "cost_time": resp.cost_time,
                "raw_content": resp.content[:200]
            })
    
    # 生成报告
    total = len(results)
    if total == 0:
        print("没有找到测试样本，请检查 manifest 路径")
        return
    success_rate = sum(1 for r in results if r['success']) / total
    json_rate = sum(1 for r in results if r['valid_json']) / total
    avg_conf = sum(r['confidence'] for r in results if r['valid_json']) / max(1, sum(1 for r in results if r['valid_json']))
    
    report = f"""
=== 手写极限测试报告 ===
样本总数: {total}
调用成功率: {success_rate:.1%}
JSON 格式合规率: {json_rate:.1%}
平均置信度: {avg_conf:.2f}

失败样本列表:
"""
    for r in results:
        if not r['success'] or not r['valid_json']:
            report += f"\n- {r['filename']} (难度 {r['difficulty']}): success={r['success']}, valid_json={r['valid_json']}"
    
    with open("test_report.txt", "w", encoding="utf-8") as f:
        f.write(report)
    print(report)
    
    # 保存详细结果
    with open("test_results.csv", "w", encoding="utf-8", newline='') as f:
        writer = csv.DictWriter(f, fieldnames=results[0].keys())
        writer.writeheader()
        writer.writerows(results)

if __name__ == "__main__":
    import os
    api_key = os.environ.get("ZHIPU_API_KEY", "899b1cee727b455a8c1b99464223e395.Qpzgo6tGVrQzlHdd")
    analyzer = ZhipuVisionAnalyzer(api_key)
    run_batch_test(analyzer)