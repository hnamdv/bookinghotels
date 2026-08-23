// ========================================
// booking-chart.js - Biểu đồ
// ========================================

function initChart(forecastDays, forecastData) {
    if (forecastDays && forecastDays.length > 0) {
        var ctx = document.getElementById('lineChart');
        if (ctx) {
            new Chart(ctx.getContext('2d'), {
                type: 'line',
                data: {
                    labels: forecastDays,
                    datasets: [{
                        label: 'Tỉ lệ (%)',
                        data: forecastData,
                        borderColor: '#1E6CD4',
                        backgroundColor: 'rgba(30,108,212,0.05)',
                        borderWidth: 2,
                        pointRadius: 4,
                        tension: .3,
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            max: 100
                        }
                    }
                }
            });
        }
    }
}