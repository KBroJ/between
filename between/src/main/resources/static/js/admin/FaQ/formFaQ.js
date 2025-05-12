
async function fetchData(url, options = {}) {
          try {
              const response = await fetch(url, options);
              if (!response.ok) {
                  let errorMsg = `HTTP 오류! 상태: ${response.status}`;
                  try { const errData = await response.json(); errorMsg = errData.message || errorMsg; } catch (e) {}
                  throw new Error(errorMsg);
              }
              if (response.status === 204) return null; // No Content
              return await response.json();
          } catch (error) { console.error(`Workspace 오류 (${url}):`, error); throw error; }
      }

      // --- FAQ Form 제출 이벤트 처리 ---
      const faqForm = document.getElementById('faq-form'); // 폼 요소 가져오기
      const submitButton = faqForm ? faqForm.querySelector('button[type="submit"]') : null; // 등록 버튼

      if (faqForm && submitButton) {
          faqForm.addEventListener('submit', async (event) => {
              event.preventDefault(); // HTML 기본 폼 전송 기능 막기
              event.stopPropagation();

              // 1. 폼 데이터 가져오기
              const questionInput = document.getElementById('faq-question');
              const answerInput = document.getElementById('faq-answer');

              if (!questionInput || !answerInput) {
                  alert("질문 또는 답변 입력 요소를 찾을 수 없습니다."); return;
              }

              const faqData = {
                  question: questionInput.value.trim(), // 앞뒤 공백 제거
                  answer: answerInput.value.trim()
              };

              // 2. 간단한 빈 값 유효성 검사
              if (!faqData.question || !faqData.answer) {
                  alert("질문과 답변 내용을 모두 입력해주세요.");
                  return;
              }

              // 3. 버튼 비활성화 및 로딩 표시
              const originalButtonHtml = submitButton.innerHTML;
              submitButton.disabled = true;
              submitButton.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> 등록 중...';

              // 4. CSRF 토큰 준비 (HTML <head>에 meta 태그 필요)
            const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
            const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
            const headers = { 'Content-Type': 'application/json' };
              if (csrfToken && csrfHeader) {
                  headers[csrfHeader] = csrfToken;

              } else {
                   console.warn("CSRF meta tags not found."); // CSRF 사용 안 할 경우 무시됨
              }

              try {
                  // 5. 백엔드 API 호출 (fetch 사용)
                  const apiUrl = '/api/adminFaQCreate';
                  const response = await fetch(apiUrl, {
                      method: 'POST',
                      headers: headers,
                      body: JSON.stringify(faqData) // JavaScript 객체를 JSON 문자열로 변환
                  });

                  // 6. 응답 처리
                  if (!response.ok) { // 에러 응답 (4xx, 5xx) 처리
                      // 백엔드에서 보낸 JSON 에러 메시지 파싱 시도
                      const errorResult = await response.json().catch(() => ({ message: `등록 실패 (${response.status})` }));
                      if (response.status === 400 && errorResult.errors) {
                           console.error("Validation errors:", errorResult.errors);
                           // 간단히 첫 번째 에러 메시지만 보여주기
                           const firstErrorField = Object.keys(errorResult.errors)[0];
                           const firstErrorMessage = errorResult.errors[firstErrorField];
                           throw new Error(`입력값 오류: ${firstErrorMessage}`);
                      }
                      throw new Error(errorResult.message || `FAQ 등록 실패 (${response.status})`);
                  }

                  // 성공 응답 처리 (201 Created 예상)
                  const result = await response.json();
                  console.log("FAQ 등록 성공:", result);
                  alert('FAQ가 성공적으로 등록되었습니다.');
                  // 목록 페이지로 이동
                  window.location.href = '/admin/faq_admin';

              } catch (error) {
                  console.error('FAQ 등록 처리 중 오류:', error);
                  alert(`등록 중 오류가 발생했습니다: ${error.message}`);
                  submitButton.disabled = false;
                  submitButton.innerHTML = originalButtonHtml;
              }
          });
      } else {
           console.error("FAQ 폼 또는 등록 버튼을 찾을 수 없습니다!");
      }