// Toggle dropdown
        function toggleDropdown(event, btn) {
            if (event) event.stopPropagation();
            const dropdown = btn.closest('.action-dropdown');
            const menu = dropdown.querySelector('.dropdown-menu');
            const isOpen = menu.classList.contains('show');

            // Đóng tất cả dropdown khác
            document.querySelectorAll('.action-dropdown .dropdown-menu.show').forEach(m => {
                if (m !== menu) m.classList.remove('show');
            });

            menu.classList.toggle('show');
        }

        // Đóng dropdown sau khi click action
        function closeDropdown(element) {
            const dropdown = element.closest('.action-dropdown');
            if (dropdown) {
                const menu = dropdown.querySelector('.dropdown-menu');
                if (menu) menu.classList.remove('show');
            }
        }

        // Đóng dropdown khi click bên ngoài
        document.addEventListener('click', function(e) {
            document.querySelectorAll('.action-dropdown .dropdown-menu.show').forEach(menu => {
                if (!menu.closest('.action-dropdown').contains(e.target)) {
                    menu.classList.remove('show');
                }
            });
        });
         // ===== DROPDOWN =====
                function toggleDropdown(event, btn) {
                    if (event) event.stopPropagation();
                    const dropdown = btn.closest('.action-dropdown');
                    const menu = dropdown.querySelector('.dropdown-menu');
                    const isOpen = menu.classList.contains('show');

                    document.querySelectorAll('.action-dropdown .dropdown-menu.show').forEach(m => {
                        if (m !== menu) m.classList.remove('show');
                    });

                    menu.classList.toggle('show');
                }

                function closeDropdown(element) {
                    const dropdown = element.closest('.action-dropdown');
                    if (dropdown) {
                        const menu = dropdown.querySelector('.dropdown-menu');
                        if (menu) menu.classList.remove('show');
                    }
                }

                document.addEventListener('click', function(e) {
                    document.querySelectorAll('.action-dropdown .dropdown-menu.show').forEach(menu => {
                        if (!menu.closest('.action-dropdown').contains(e.target)) {
                            menu.classList.remove('show');
                        }
                    });
                });