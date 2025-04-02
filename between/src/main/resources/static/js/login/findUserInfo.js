document.addEventListener('DOMContentLoaded', function() {

    // 탭 전환
    const tabs = document.querySelectorAll('.tab');
    const forms = document.querySelectorAll('.form-container');

    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const target = this.dataset.tab;

            tabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');

            forms.forEach(form => form.classList.remove('active'));
            document.getElementById(`${target}-form`).classList.add('active');
        });
    });


});
