// Basic JS Example to show/hide form (Replace with proper modal logic)
const addBtn = document.getElementById('add-banner-btn');
const modal = document.getElementById('banner-form-modal');
const closeBtn = modal.querySelector('.close-btn');
const cancelBtn = modal.querySelector('.cancel-btn');
const formTitle = document.getElementById('form-title');
const bannerForm = document.getElementById('banner-form');
const bannerIdInput = document.getElementById('banner-id');


addBtn.addEventListener('click', () => openModal(false));
closeBtn.addEventListener('click', closeModal);
cancelBtn.addEventListener('click', closeModal);

// Add event listeners for Edit buttons (requires querying all edit buttons)
document.querySelectorAll('.edit-btn').forEach(button => {
    button.addEventListener('click', (e) => {
        const bannerId = e.target.dataset.id;
        // TODO: Fetch banner data using bannerId
        const mockData = { id: bannerId, title: `배너 ${bannerId} 이름`, url: `/link/${bannerId}`, status: 'active' }; // Example data
        openModal(true, mockData);
    });
});

// Close modal if clicking outside of it
window.addEventListener('click', (event) => {
    if (event.target == modal) {
        closeModal();
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

// Handle Image Preview (Basic Example)
const imageInput = document.getElementById('banner-image');
const preview = document.getElementById('image-preview');
imageInput.addEventListener('change', function() {
    const file = this.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            preview.src = e.target.result;
            preview.style.display = 'block';
        }
        reader.readAsDataURL(file);
    } else {
        preview.src = '#';
        preview.style.display = 'none';
    }
});

// NOTE: Drag & Drop requires a library like SortableJS or implementing complex JS.
// NOTE: Delete confirmation and actual deletion require JS + Backend.
// NOTE: Form submission requires JS (preventDefault, FormData, fetch/AJAX) + Backend.