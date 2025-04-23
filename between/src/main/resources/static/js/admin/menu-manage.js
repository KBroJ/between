$(function () {
    //jstree
    $('#jstree_div').jstree({
        'core': {
            'data': function (node, callback) {
                getTreeData(node.id, callback);
            }
        }
    })
    //노드선택 이벤트
        .on("select_node.jstree", function (e, data) {
            const nodeId = data.node.id;
            const nodeText = data.node.text;

            console.log("nodeId => ", nodeId);
            console.log("nodeText => ", nodeText);

            // 가상 루트 노드는 선택 시 폼 초기화 또는 다른 처리
            if (nodeId.startsWith('VIRTUAL_')) {
                resetForm();
                $('#upperMenuNo').val(''); // 상위 ID 없음
                $('#upperMenuDisplay').val(''); // 상위 이름 없음
                // 특정 타입의 최상위 메뉴를 추가하기 위한 준비 가능
                console.log('Selected virtual root:', nodeText);
                return;
            }

            // 실제 메뉴 노드 선택 시 상세 정보 로드 및 폼 채우기
            loadMenuDetails(nodeId);
        })
    ;

    /**
     * 트리데이터 조회
     * @param nodeId
     * @param callback
     */
    function getTreeData(nodeId, callback) {
        console.log(nodeId);

        $.ajax({
            url: '/admin/menus/root',
            type: 'GET',
            data: { id : nodeId },
            dataType: 'json',
            success: function (data) {
                console.log(data);
                callback(data);  // ⬅ 여기서 트리 UI에 데이터를 넘기는 것!
            },
            error: function (data) {
                console.log(data);
            }
        });
    }

    // 추가 버튼 클릭
    $('#addBtn').on('click', function() {
        prepareNewMenu();
    });
}); // End of $(function() { ... });

/**
 * 폼을 초기화합니다.
 */
function resetForm() {
    $('#menuInfoForm')[0].reset(); // 폼 요소 기본값으로 리셋
    $('#menuNo').val('');        // 숨겨진 ID 필드 클리어
    $('#upperMenuNo').val('');   // 숨겨진 상위 ID 필드 클리어
    $('#upperMenuDisplay').val(''); // 표시용 상위 필드 클리어
    $('input[name="useAt"][value="Y"]').prop('checked', true); // 기본값 '노출' 선택
}

/**
 * 메뉴 상세 정보를 로드하여 폼에 채웁니다. (AJAX 필요)
 * @param {string} menuId 조회할 메뉴의 ID (menuNo)
 */
function loadMenuDetails(menuId) {
    console.log("loadDetail => ", menuId);

    $.ajax({
        url: `/admin/menus/details/${menuId}`,
        type: 'GET',
        dataType: 'json',
        success: function (data) {
            console.log("detail result => ", data);

            $('#menuNo').val(data.menuNo);
            $('#menuNm').val(data.menuNm);
            $('#upperMenuNo').val(data.upperMenuNo === '#' ? '' : data.upperMenuNo); // 루트는 상위 없음
            $('#upperMenuDisplay').val(data.upperMenuNo === '#' ? '최상위 메뉴' : data.upperMenuNm);
            $('#sortOrder').val(data.sortOrder);
            $('input[name="useAt"][value="' + data.useAt + '"]').prop('checked', true);
            $('#menuUrl').val(data.menuUrl);
            $('#menuDsc').val(data.menuDsc);


        },
        error: function (data) {
            console.log(data);
        }
    })

    // --- AJAX 성공 시 콜백 함수 내에서 아래 로직 실행 ---
    // 가상의 데이터로 폼 채우기 (실제로는 AJAX 응답 사용)
    const menuData = { // <<-- 이 부분은 실제 AJAX 응답 데이터로 대체되어야 함
        menuNo: menuId,
        menuNm: $('#jstree_div').jstree(true).get_node(menuId).text, // 트리에서 이름 가져오기 (임시)
        upperMenuNo: $('#jstree_div').jstree(true).get_parent(menuId), // 트리에서 부모 ID 가져오기 (임시)
        upperMenuNm: $('#jstree_div').jstree(true).get_node($('#jstree_div').jstree(true).get_parent(menuId)).text, // 트리에서 부모 이름 (임시)
    };


    // --- 폼 채우기 끝 ---


}

/**
 * 새 메뉴 추가를 위해 폼을 준비합니다.
 */
function prepareNewMenu() {
    const selectedNode = $('#jstree_div').jstree('get_selected', true); // 선택된 노드 객체 가져오기
    resetForm(); // 먼저 폼 초기화
    if (selectedNode.length > 0) {
        // 선택된 노드가 있으면 해당 노드를 부모로 설정
        let parentNode = selectedNode[0];

        // 가상 루트 노드를 부모로 선택한 경우 처리
        if(parentNode.id.startsWith('VIRTUAL_')) {
            $('#upperMenuNo').val(''); // 실제 부모 ID 없음
            $('#upperMenuDisplay').val(parentNode.text + ' 아래 최상위'); // 표시용
            // 실제 저장 시 menuType은 parentNode.data.type 사용
        } else {
            $('#upperMenuNo').val(parentNode.id); // 선택된 노드 ID를 상위 ID로
            $('#upperMenuDisplay').val(parentNode.text); // 선택된 노드 이름을 표시
        }
    } else {
        // 선택된 노드가 없으면 최상위 메뉴로 추가 (어떤 타입인지는 별도 지정 필요)
        alert("상위 메뉴를 트리에서 선택하거나, 타입을 지정해야 합니다.");
        $('#upperMenuDisplay').val('최상위 메뉴 (타입 지정 필요)');
    }
    $('#menuNm').focus(); // 메뉴명 입력 필드에 포커스
    console.log('Prepared for new menu.');

}

/**
 * 폼 데이터를 서버에 저장(생성 또는 수정)합니다. (AJAX 필요)
 */
function saveMenu() {
    let menuData = {};
    // 폼 데이터를 객체로 수집 (jQuery serializeArray 또는 직접 수집)
    $('#menuInfoForm').serializeArray().forEach(function(item) {
        menuData[item.name] = item.value;
    });

    const menuId = $('#menuNo').val(); // 숨겨진 menuNo 값 확인
    const isNew = !menuId; // menuNo가 없으면 새 메뉴

    const url = isNew ? '/admin/menus' : '/admin/menus/' + menuId;
    const method = isNew ? 'POST' : 'PUT';

    console.log('Saving menu:', method, url, menuData);

    // TODO: 실제 AJAX 요청 보내기
    // 예: $.ajax({ url: url, method: method, contentType: 'application/json', data: JSON.stringify(menuData), success: function(response) { ... } });

    // --- AJAX 성공 시 콜백 ---
    alert('메뉴가 ' + (isNew ? '추가' : '수정') + '되었습니다.');
    // 트리 갱신 또는 리로드
    const parentNodeId = menuData.upperMenuNo || '#'; // 상위 ID 없으면 루트 아래
    // 특정 노드만 리프레시 하거나, 전체 리프레시
    // $('#menuTreeContainer').jstree('refresh_node', parentNodeId); // 특정 부모만
    $('#jstree_div').jstree('refresh'); // 전체 리프레시 (간단하지만 비효율적일 수 있음)

    // 만약 새로 생성된 경우, 생성된 노드를 선택하거나 폼 업데이트 가능
    // if (isNew && response && response.menuNo) {
    //     loadMenuDetails(response.menuNo); // 저장 후 데이터 다시 로드
    //     // JSTree에서 해당 노드 선택하는 로직 추가 가능
    // }
    // --- 성공 콜백 끝 ---
}
