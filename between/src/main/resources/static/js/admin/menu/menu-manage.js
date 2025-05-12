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

        $.ajax({
            url: '/admin/menus/root',
            type: 'GET',
            data: { nodeId : nodeId },
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

    // 저장
    $('#saveBtn').on('click', function() {
        saveMenu();
    });

    //삭제
    $('#deleteBtn').on('click', function() {
        deleteMenu();
    })

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
 * @param {string} menuNo 조회할 메뉴의 ID (menuNo)
 */
function loadMenuDetails(menuNo) {
    console.log("loadDetail => ", menuNo);

    $.ajax({
        url: `/admin/menus/details/${menuNo}`,
        type: 'GET',
        dataType: 'json',
        success: function (data) {
            console.log("detail result => ", data);
            console.log("detail result menuNo => ", data.adminMenuResDto.menuNo);
            $('#menuNo').val(data.adminMenuResDto.menuNo);
            $('#menuNm').val(data.adminMenuResDto.menuNm);
            $('#upperMenuNo').val(data.adminMenuResDto.upperMenuNo === '#' ? '' : data.adminMenuResDto.upperMenuNo); // 루트는 상위 없음
            $('#upperMenuDisplay').val(data.adminMenuResDto.upperMenuNo === '#' ? '최상위 메뉴' : data.adminMenuResDto.upperMenuNo);
            $('#sortOrder').val(data.adminMenuResDto.sortOrder);
            $('input[name="useAt"][value="' + data.adminMenuResDto.useAt + '"]').prop('checked', true);
            $('#menuUrl').val(data.adminMenuResDto.menuUrl);
            $('#menuDsc').val(data.adminMenuResDto.menuDsc);
            $('#menuType').val(data.adminMenuResDto.menuType);

            //메뉴 역할 정보 표출 메소드
            populateRoleCheckboxes(data.allRoleDto, data.assignedRoleIds)
        },
        error: function (data) {
            console.log(data);
        }
    })

}

// 역할 체크박스 생성 함수
function populateRoleCheckboxes(allRoles, assignedRoleIds = []) { // assignedRoleIds 기본값 빈 배열
    const container = $('#roleCheckboxes');
    container.empty(); // 기존 내용 비우기

    if (!allRoles || allRoles.length === 0) {
        container.text('정의된 역할이 없습니다.');
        return;
    }

    allRoles.forEach(role => {
        const checkboxId = 'role_' + role.roleId;
        const isChecked = assignedRoleIds.includes(role.roleId); // 역할 ID가 할당 목록에 있는지 확인

        const checkbox = $('<input>')
            .attr('type', 'checkbox')
            .attr('name', 'allowedRoles')
            .attr('id', checkboxId)
            .val(role.roleId)
            .prop('checked', isChecked); // 체크 상태 설정

        const label = $('<label>')
            .attr('for', checkboxId)
            .text(role.roleName);

        // 스타일링을 위해 div로 감싸기 (선택적)
        const div = $('<div>').addClass('checkbox-item');
        div.append(checkbox).append(label);
        container.append(div);
    });
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
        if(!/^\d+$/.test(parentNode.id)) {
            $('#upperMenuNo').val(''); // 실제 부모 ID 없음
            $('#upperMenuDisplay').val(parentNode.text + ' 아래 최상위'); // 표시용
            $('#menuType').val(parentNode.id);
            // 실제 저장 시 menuType은 parentNode.data.type 사용
        } else {
            $('#upperMenuNo').val(parentNode.id); // 선택된 노드 ID를 상위 ID로
            $('#upperMenuDisplay').val(parentNode.text); // 선택된 노드 이름을 표시
        }

        //메뉴타입 설정은 선택 상단


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

    const menuNo = $('#menuNo').val(); // 숨겨진 menuNo 값 확인
    const isNew = !menuNo; // menuNo가 없으면 새 메뉴

    console.log("isNew = ", isNew);
    console.log("menuNo = ", menuNo);
    console.log("menyType = ", $("#menuType").val());
    const url = isNew ? '/admin/menus/regist' : '/admin/menus/edit/' + menuNo;
    const method = isNew ? 'POST' : 'PUT';

    console.log('Saving menu:', method, url, menuData);
    console.log(JSON.stringify(menuData));
    // TODO: 실제 AJAX 요청 보내기
    $.ajax({
        url: url,
        method: method,
        contentType: 'application/json',
        data: JSON.stringify(menuData),
        success: function (data) {
            alert('메뉴가 ' + (isNew ? '추가' : '수정') + '되었습니다.');
            // 트리 갱신 또는 리로드
            // const parentNodeId = menuData.upperMenuNo || '#'; // 상위 ID 없으면 루트 아래
            // 특정 노드만 리프레시 하거나, 전체 리프레시
            // $('#menuTreeContainer').jstree('refresh_node', parentNodeId); // 특정 부모만
            $('#jstree_div').jstree('refresh'); // 전체 리프레시 (간단하지만 비효율적일 수 있음)
        },
        error: function (data) {
            alert("메뉴 등록에 실패하였습니다.")
        }
    })


    // 만약 새로 생성된 경우, 생성된 노드를 선택하거나 폼 업데이트 가능
    // if (isNew && response && response.menuNo) {
    //     loadMenuDetails(response.menuNo); // 저장 후 데이터 다시 로드
    //     // JSTree에서 해당 노드 선택하는 로직 추가 가능
    // }
    // --- 성공 콜백 끝 ---
}

/**
 * 삭제
 */
function deleteMenu() {
    const menuNo = $('#menuNo').val(); // 숨겨진 menuNo 값 확인
    console.log("deleteMenu => ", menuNo);
    if (confirm('정말 이 역할을 삭제하시겠습니까? (ID: ' + menuNo + ')')) {

        $.ajax({
            url:`/admin/menus/delete/${menuNo}`,
            type: 'DELETE',
            success: function (data) {
                alert('메뉴가 삭제 되었습니다.');
                // 트리 갱신 또는 리로드
                $('#jstree_div').jstree('refresh'); // 전체 리프레시 (간단하지만 비효율적일 수 있음)
            },
            error: function (data) {
                alert('메뉴가 삭제에 실패하였습니다.');
            }
        })
    }

}
