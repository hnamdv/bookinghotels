// ========================================
// booking-chart.js - Biểu đồ thống kê
// ========================================

let bookingChart = null;

function initChart(days, data) {
    const canvas = document.getElementById('lineChart');
    if (!canvas) {
        console.warn('Canvas lineChart not found');
        return;
    }

    const ctx = canvas.getContext('2d');

    // Hủy chart cũ nếu có
    if (bookingChart) {
        bookingChart.destroy();
    }

    // Đảm bảo data là array
    const labels = Array.isArray(days) ? days : [];
    const values = Array.isArray(data) ? data : [];

    // Nếu không có data, tạo data mặc định
    if (labels.length === 0) {
        const today = new Date();
        const formatter = new Intl.DateTimeFormat('en-US', { weekday: 'short' });
        for (let i = 0; i < 7; i++) {
            const date = new Date(today);
            date.setDate(date.getDate() + i);
            labels.push(formatter.format(date));
            values.push(0);
        }
    }

    // Tạo gradient fill
    const gradient = ctx.createLinearGradient(0, 0, 0, 200);
    gradient.addColorStop(0, 'rgba(30, 108, 212, 0.3)');
    gradient.addColorStop(0.5, 'rgba(30, 108, 212, 0.15)');
    gradient.addColorStop(1, 'rgba(30, 108, 212, 0.02)');

    bookingChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Tỉ lệ lấp đầy',
                data: values,
                borderColor: '#1E6CD4',
                backgroundColor: gradient,
                borderWidth: 2.5,
                fill: true,
                tension: 0.4,
                pointBackgroundColor: '#1E6CD4',
                pointBorderColor: '#ffffff',
                pointBorderWidth: 2,
                pointRadius: 5,
                pointHoverRadius: 8,
                pointHoverBackgroundColor: '#1E6CD4',
                pointHoverBorderColor: '#ffffff',
                pointHoverBorderWidth: 3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: 'rgba(30, 41, 59, 0.9)',
                    titleColor: '#ffffff',
                    bodyColor: '#ffffff',
                    padding: 12,
                    cornerRadius: 8,
                    displayColors: false,
                    callbacks: {
                        label: function(context) {
                            return 'Tỉ lệ lấp đầy: ' + context.parsed.y + '%';
                        },
                        title: function(context) {
                            return 'Ngày ' + context[0].label;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100,
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)',
                        drawBorder: false
                    },
                    ticks: {
                        callback: function(value) {
                            return value + '%';
                        },
                        stepSize: 25,
                        font: {
                            size: 10,
                            weight: '500'
                        },
                        color: '#64748B'
                    },
                    title: {
                        display: false
                    }
                },
                x: {
                    grid: {
                        display: false,
                        drawBorder: false
                    },
                    ticks: {
                        font: {
                            size: 11,
                            weight: '600'
                        },
                        color: '#64748B'
                    }
                }
            }
        }
    });
}

// Cập nhật chart khi có dữ liệu mới
function updateChart(days, data) {
    if (bookingChart) {
        // Cập nhật data mà không cần tạo lại chart
        bookingChart.data.labels = days || [];
        bookingChart.data.datasets[0].data = data || [];
        bookingChart.update();
    } else {
        // Tạo mới chart
        initChart(days, data);
    }
}

// Xuất hàm ra global
window.initChart = initChart;
window.updateChart = updateChart;