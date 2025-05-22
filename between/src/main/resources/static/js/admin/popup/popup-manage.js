
$(document).ready(function () {
    const tableBody = $('#resultsBody');
    if(tableBody) {
        tableBody.on('click', '.delete-button', function () {
            const delButton = $(this);
            const popupId = delButton.data('popupid');
            console.log("popupId => ", popupId);

            if(confirm('정말 이 권한를 삭제하시겠습니까? (ID: ' + popupId + ')')) {
                $.ajax({
                    url: `/admin/popup/delete/${popupId}`,
                    type: 'DELETE',
                    success: function (data) {
                        alert("삭제에 성공했습니다.")
                        location.reload();
                    },
                    error: function (data) {
                        alert("삭제에 실패했습니다.")
                    }
                });
            }
        });
    }
});



// Handle Status Toggle Text (Basic Example)
document.querySelectorAll('.status-toggle').forEach(toggle => {
    toggle.addEventListener('change', (e) => {
        const statusTextSpan = e.target.closest('td').querySelector('.status-text');
        if (e.target.checked) {
            statusTextSpan.textContent = '활성';
            statusTextSpan.className = 'status-text status-active';
        } else {
            statusTextSpan.textContent = '비활성';
            statusTextSpan.className = 'status-text status-inactive';
        }
        // TODO: Add AJAX call here to update status on the server
        console.log(`Banner ID ${e.target.dataset.id} status changed to ${e.target.checked ? 'active' : 'inactive'}`);
    });
});
//
// // Handle Image Preview (Basic Example)
// const imageInput = document.getElementById('banner-image');
// const preview = document.getElementById('image-preview');
// imageInput.addEventListener('change', function() {
//     const file = this.files[0];
//     if (file) {
//         const reader = new FileReader();
//         reader.onload = function(e) {
//             preview.src = e.target.result;
//             preview.style.display = 'block';
//         }
//         reader.readAsDataURL(file);
//     } else {
//         preview.src = '#';
//         preview.style.display = 'none';
//     }
// });