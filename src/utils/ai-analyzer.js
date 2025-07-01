
// AI analysis utility using Gemini API
export class AIAnalyzer {
    constructor(toastManager) {
        this.toastManager = toastManager;
    }

    async analyzeImage(imageData, apiKey) {
        // StorageManager를 직접 임포트해서 사용
        if (!apiKey) {
            try {
                const StorageManager = await import('./storage.js').then(module => module.StorageManager);
                apiKey = StorageManager.getApiKey();
            } catch (error) {
                console.error('StorageManager 로드 오류:', error);
            }
        }

        if (!apiKey) {
            this.toastManager.showToast('Gemini API 키가 필요합니다. 설정에서 등록해주세요 🌽', 'warning');
            return null;
        }

        if (!imageData) {
            this.toastManager.showToast('분석할 이미지가 없습니다 🌽', 'error');
            return null;
        }

        const aiAnalysis = document.getElementById('ai-analysis');
        if (aiAnalysis) {
            aiAnalysis.classList.remove('hidden');
        }

        try {
            const base64Data = imageData.split(',')[1];
            
            const requestBody = {
                contents: [{
                    parts: [
                        {
                            text: "이 기프티콘 이미지를 분석해서 다음 정보를 JSON 형식으로 추출해주세요:\n" +
                                  "- brand: 브랜드명 (예: 스타벅스, 이디야커피)\n" +
                                  "- name: 상품명 (예: 아메리카노 Tall)\n" +
                                  "- amount: 금액 (숫자만, 예: 4500)\n" +
                                  "- expiry: 유효기간 (YYYY-MM-DD 형식)\n" +
                                  "정보를 찾을 수 없으면 null로 설정해주세요. 응답은 순수 JSON만 해주세요."
                        },
                        {
                            inline_data: {
                                mime_type: "image/jpeg",
                                data: base64Data
                            }
                        }
                    ]
                }]
            };

            const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(`API 오류: ${response.status}`);
            }

            const data = await response.json();
            console.log('Gemini API 응답:', data);
            
            if (data.candidates && data.candidates[0] && data.candidates[0].content) {
                const text = data.candidates[0].content.parts[0].text;
                console.log('추출된 텍스트:', text);
                
                try {
                    const jsonMatch = text.match(/\{[\s\S]*\}/);
                    if (jsonMatch) {
                        const couponInfo = JSON.parse(jsonMatch[0]);
                        this.toastManager.showToast('AI 분석 완료! 정보를 확인해주세요 🌽🤖', 'success');
                        return couponInfo;
                    } else {
                        throw new Error('JSON 형식을 찾을 수 없음');
                    }
                } catch (parseError) {
                    console.error('JSON 파싱 오류:', parseError);
                    this.toastManager.showToast('AI 분석 결과를 처리할 수 없습니다. 수동으로 입력해주세요 🌽', 'warning');
                    return null;
                }
            } else {
                throw new Error('API 응답이 비어있습니다');
            }

        } catch (error) {
            console.error('AI 분석 오류:', error);
            this.toastManager.showToast('AI 분석에 실패했습니다. 수동으로 입력해주세요 🌽', 'warning');
            return null;
        } finally {
            if (aiAnalysis) {
                aiAnalysis.classList.add('hidden');
            }
        }
    }
}
