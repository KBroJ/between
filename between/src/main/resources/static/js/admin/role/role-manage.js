$(document).ready(function(){
    const tableBody = $('#resultsBody');

    if(tableBody) {
        tableBody.on('click', '.btn-delete', function () {
            const delButton = $(this);
            const roleId = delButton.data('role-id');
            console.log("roleId => ", roleId);

            if (confirm('정말 이 역할을 삭제하시겠습니까? (ID: ' + roleId + ')')) {

                $.ajax({
                    url:`/admin/roles/delete/${roleId}`,
                    type: 'DELETE',
                    success: function (data) {
                        alert("삭제에 성공했습니다.")
                        location.reload();
                    },
                    error: function (data) {
                        alert("삭제에 실패했습니다.")
                    }
                })
            }

        })
    }

})