 // --- DOM 요소 ---
const faqTableBody = document.getElementById('faq-list-body');
const editFaqModalElement = document.getElementById('editFaqModal');
const editFaqForm = document.getElementById('faq-edit-modal-form');
const editFaqQNoInput = document.getElementById('edit-faq-qno');
const editFaqQuestionInput = document.getElementById('edit-faq-question');
const editFaqAnswerInput = document.getElementById('edit-faq-answer');
const editFaqLoadingDiv = document.getElementById('edit-faq-loading');
const editFaqErrorDiv = document.getElementById('edit-faq-error');
const saveFaqChangesBtn = document.getElementById('saveFaqChangesBtn');

let editFaqModalInstance = null; // Bootstrap Modal 객체 저장용

/** 서버 API 호출 함수 */
async function fetchData(url, options = {}) {
    try {
        const response = await fetch(url, options);
        if (!response.ok) {
            let errorMsg = `HTTP 오류! 상태: ${response.status}`;
            try { const errData = await response.json(); errorMsg = errData.message || errorMsg; } catch (e) {}
            throw new Error(errorMsg);
        }
        if (response.status === 204) return null;
        return await response.json();
    } catch (error) { console.error(`Workspace 오류 (${url}):`, error); throw error; }
}


        /** 백엔드 API를 호출하여 FAQ 목록을 가져와 테이블에 동적으로 표시합니다. */
        async function loadFaqs() {
            if (!faqTableBody) { console.error("FAQ 목록 tbody 요소를 찾을 수 없습니다!"); return; }
            faqTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4"><i class="fas fa-spinner fa-spin me-1"></i> FAQ 목록 로딩 중...</td></tr>`; // 로딩 표시 (colspan 수정)

            try {

                const faqList = await fetchData('/api/adminFaQList'); // GET 요청

                faqTableBody.innerHTML = '';

                if (!faqList || !Array.isArray(faqList) || faqList.length === 0) {
                    faqTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">등록된 FAQ가 없습니다.</td></tr>`; // colspan 수정
                    return;
                }

                // 각 FAQ 데이터로 테이블 행 생성
                faqList.forEach((faq, index) => {
                    const row = faqTableBody.insertRow();

                    row.insertCell().textContent = index + 1; // 번호

                    // 질문 셀 (링크 포함)
                    const cellQuestion = row.insertCell();
                    cellQuestion.className = 'faq-question-cell';
                    const questionLink = document.createElement('a');
                    // questionLink.href = `/admin/faqs/edit/${faq.qno}`;
                    questionLink.textContent = faq.question || '(내용 없음)';
                    questionLink.className = 'text-decoration-none text-dark';
                    cellQuestion.appendChild(questionLink);

                    // 등록일 셀
                    const cellDate = row.insertCell();
                    try { // 날짜 포맷팅
                         cellDate.textContent = faq.createdAt ? new Date(faq.createdAt).toLocaleDateString('ko-KR') : '-';
                    } catch(e) { cellDate.textContent = faq.createdAt || '-'; }


                    console.log(`  [loadFaqs] Setting button data for FAQ: ID=${faq.qno}, Type=${typeof faq.qno}`);


                    // 관리 버튼 셀
                    const cellActions = row.insertCell();
                    const editButton = document.createElement('button');
                    editButton.dataset.faqId = faq.qno;
                    cellActions.className = 'text-center action-buttons';
                    const deleteButton = document.createElement('button');
                    deleteButton.type = 'button';
                    deleteButton.className = 'btn btn-sm btn-outline-danger';
                    deleteButton.title = '삭제';
                    deleteButton.innerHTML = '<i class="fas fa-trash-alt"></i>';
                    deleteButton.dataset.faqId = faq.qno;
                    deleteButton.dataset.faqQuestion = faq.question;
                    deleteButton.onclick = () => confirmDeleteFaq(faq.qno, faq.question);
                    cellActions.appendChild(deleteButton);


                    editButton.type = 'button'; editButton.className = 'btn btn-sm btn-outline-secondary';
                    editButton.title = '수정'; editButton.innerHTML = '<i class="fas fa-edit"></i>';
                    editButton.dataset.bsToggle = 'modal';         // Bootstrap 속성
                    editButton.dataset.bsTarget = '#editFaqModal'; // Modal ID

                    cellActions.appendChild(editButton);

                });

            } catch (error) {
                console.error("FAQ 목록 로드 또는 렌더링 실패:", error);
                faqTableBody.innerHTML = `<tr><td colspan="4" class="text-center text-danger py-4">FAQ 목록을 불러오는 중 오류가 발생했습니다.</td></tr>`; // colspan 수정
            }
        }


        /** 페이지 로드 완료 시 실행 */
        document.addEventListener('DOMContentLoaded', function() {
        console.log("DOM 로드 완료. FAQ 관리 페이지 초기화 시작.");

        // Bootstrap Modal 인스턴스 생성 (최초 1회)
        if (editFaqModalElement) {
             editFaqModalInstance = new bootstrap.Modal(editFaqModalElement);
             // Modal 열릴 때 이벤트 리스너 등록
             editFaqModalElement.addEventListener('show.bs.modal', loadFaqDataIntoModal);
        } else { console.warn("수정 Modal 요소를 찾을 수 없습니다."); }

        // Modal 내부 폼 submit 이벤트 리스너 등록
        if (editFaqForm) {
             editFaqForm.addEventListener('submit', handleEditFormSubmit);
        } else { console.warn("수정 Modal Form 요소를 찾을 수 없습니다."); }

        // FAQ 목록 초기 로드
        loadFaqs();
        });

        /** Modal이 열리기 직전에 FAQ 상세 데이터 로드 및 폼 채우기 */
        async function loadFaqDataIntoModal(event) {
            if (!editFaqForm || !editFaqLoadingDiv || !editFaqErrorDiv || !editFaqQNoInput || !editFaqQuestionInput || !editFaqAnswerInput || !saveFaqChangesBtn) return;
            const button = event.relatedTarget;
            const faqId = button.dataset.faqId;
            console.log("수정 Modal 열기, ID:", faqId, "(타입:", typeof faqId + ")"); // 타입 확인


            // UI 초기화
            editFaqForm.style.display = 'none'; editFaqErrorDiv.style.display = 'none';
            editFaqLoadingDiv.style.display = 'block'; saveFaqChangesBtn.disabled = true;

            if (!faqId) { console.error("오류: faqId가 버튼 dataset에서 누락되었거나 undefined입니다"); return; }

            try {
                const apiUrl = `/api/admin/faqs/${faqId}`;
                const faqData = await fetchData(apiUrl);

                if (!faqData) { throw new Error("FAQ 데이터 없음"); }

                // 폼 채우기 (DTO 필드명 확인!)
                editFaqQNoInput.value = faqData.qno || faqId;
                editFaqQuestionInput.value = faqData.question || '';
                editFaqAnswerInput.value = faqData.answer || '';

                // UI 표시
                editFaqForm.style.display = 'block'; editFaqLoadingDiv.style.display = 'none';
                saveFaqChangesBtn.disabled = false;

            } catch (error) {
                console.error("Modal FAQ 데이터 로드 실패:", error);
                editFaqErrorDiv.textContent = `FAQ 정보 로드 실패: ${error.message}`;
                editFaqErrorDiv.style.display = 'block';
                editFaqLoadingDiv.style.display = 'none';
            }
        }


        /** FAQ 삭제 확인 및 API 호출 함수 */
            function confirmDeleteFaq(faqId, faqQuestion) {
                const questionPreview = faqQuestion && faqQuestion.length > 30 ? faqQuestion.substring(0, 30) + '...' : faqQuestion;
                if (confirm(`[${questionPreview || '선택된 FAQ'}] 항목을 정말 삭제하시겠습니까?\n삭제 후에는 복구할 수 없습니다.`)) {
                    console.log("삭제 API 호출 시도 - ID:", faqId);

                    const deleteUrl = `/api/admin/faqs/${faqId}`;

                    // CSRF 토큰 준비 (HTML <head>에 meta 태그 필요)
                    const csrfToken = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
                    const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");
                    const headers = {
                        // 'Content-Type': 'application/json'
                    };
                    if (csrfToken && csrfHeader) {
                        headers[csrfHeader] = csrfToken;
                    } else {
                        console.warn("CSRF 토큰 또는 헤더를 찾을 수 없습니다. CSRF 없이 요청을 보냅니다.");
                    }

                    fetch(deleteUrl, {
                        method: 'DELETE', // HTTP DELETE 메소드 사용
                        headers: headers
                    })
                    .then(response => {
                         if (response.ok) { // 200-299 범위의 성공 상태 코드
                             // 204 No Content의 경우 response.json() 호출 시 에러 발생 가능
                             if (response.status === 204) {
                                 return null; // 내용 없으면 null 반환
                             }
                             return response.json(); // 백엔드가 JSON 응답을 보낼 경우
                         } else {
                              // 서버 에러 응답 처리 (4xx, 5xx)
                              // 백엔드가 JSON 형태의 에러 메시지를 보낸다고 가정
                              return response.json().then(err => {
                                  // err.message 가 없다면 기본 메시지 사용
                                  throw new Error(err.message || `삭제 요청 실패 (${response.status})`);
                              }).catch(() => { // JSON 파싱 실패 시 (예: HTML 에러 페이지)
                                  throw new Error(`삭제 요청에 실패했습니다. (상태: ${response.status})`);
                              });
                         }
                     })
                     .then(result => { // result는 response.json()의 결과 또는 null(204의 경우)
                          alert("FAQ가 성공적으로 삭제되었습니다.");
                          loadFaqs(); // 삭제 성공 시 목록 새로고침
                     })
                     .catch(error => {
                          console.error("FAQ 삭제 오류:", error);
                          alert(`삭제 중 오류가 발생했습니다: ${error.message}`);
                     });
                }
            }


        /** Modal 수정 폼 제출 처리 */
        async function handleEditFormSubmit(event) {
             event.preventDefault();
             if (!editFaqForm || !saveFaqChangesBtn) return;

             const qNo = editFaqQNoInput.value;
             const updatedData = { // 백엔드 수정 API가 받을 데이터
                 question: editFaqQuestionInput.value.trim(),
                 answer: editFaqAnswerInput.value.trim()

             };
             if (!qNo || !updatedData.question || !updatedData.answer) { alert("질문/답변 필수"); return; }

             console.log("FAQ 수정 저장 시도 - ID:", qNo, updatedData);
             saveFaqChangesBtn.disabled = true; saveFaqChangesBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> 저장 중...';
             editFaqErrorDiv.style.display = 'none';

             // CSRF 토큰 준비
            const csrfTokenElement = document.querySelector("meta[name='_csrf']");
            const csrfHeaderElement = document.querySelector("meta[name='_csrf_header']");
            const headers = { 'Content-Type': 'application/json' }; // 기본 헤더

     /*      if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken; // headers 객체에 CSRF 토큰 추가
                console.log(`CSRF 헤더 추가됨 (PUT 요청): ${csrfHeader} = ${csrfToken}`); // 로그 확인!
            } else {
                 console.warn("CSRF 메타 태그를 찾을 수 없거나 비어있습니다. (PUT 요청)");
            }*/



             try {
                  const updateUrl = `/api/admin/faqs/${qNo}`;
                  const response = await fetch(updateUrl, { method: 'PUT', headers: headers, body: JSON.stringify(updatedData) });

                  if (!response.ok) { const err = await response.json().catch(()=>({message: `수정 실패(${response.status})`})); throw new Error(err.message); }

                  alert("FAQ 수정 완료.");
                  editFaqModalInstance.hide(); // Modal 닫기
                  loadFaqs(); // 목록 새로고침

             } catch (error) {
                  console.error("FAQ 수정 실패:", error);
                  editFaqErrorDiv.textContent = `수정 실패: ${error.message}`;
                  editFaqErrorDiv.style.display = 'block';
                  saveFaqChangesBtn.disabled = false; // 버튼 복원
                  saveFaqChangesBtn.innerHTML = '수정 완료';
             }
        }