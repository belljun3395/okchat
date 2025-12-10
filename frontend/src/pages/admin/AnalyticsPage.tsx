import React, { useState, useEffect } from 'react';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend,
    Filler
} from 'chart.js';
import type { ScriptableContext } from 'chart.js';
import { Line } from 'react-chartjs-2';
import { analyticsService } from '../../services';

// Register ChartJS components
ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend,
    Filler
);

interface UsageStats {
    totalInteractions: number;
    averageResponseTime: number;
}

interface QualityStats {
    averageRating: number;
    helpfulPercentage: number;
}

interface QueryTypeStat {
    queryType: string;
    count: number;
    averageResponseTime: number;
    averageRating: number;
}

const AnalyticsPage: React.FC = () => {
    const [loading, setLoading] = useState(true);
    const [usageStats, setUsageStats] = useState<UsageStats>({ totalInteractions: 0, averageResponseTime: 0 });
    const [qualityStats, setQualityStats] = useState<QualityStats>({ averageRating: 0, helpfulPercentage: 0 });
    const [queryTypeStats, setQueryTypeStats] = useState<QueryTypeStat[]>([]);
    const [dateRange, setDateRange] = useState('7days');

    // Chart Data State
    const [chartData, setChartData] = useState<any>({
        labels: [],
        datasets: []
    });

    useEffect(() => {
        loadAnalytics();
    }, [dateRange]);

    const loadAnalytics = async () => {
        setLoading(true);

        // Calculate dates based on range
        const endDate = new Date();
        const startDate = new Date();
        if (dateRange === '7days') {
            startDate.setDate(endDate.getDate() - 7);
        } else if (dateRange === '30days') {
            startDate.setDate(endDate.getDate() - 30);
        } else if (dateRange === 'month') {
            startDate.setDate(1); // First day of current month
        }

        const startStr = startDate.toISOString().slice(0, 19);
        const endStr = endDate.toISOString().slice(0, 19);

        try {
            await Promise.all([
                fetchUsageStats(startStr, endStr),
                fetchQualityStats(startStr, endStr),
                fetchQueryTypeStats(startStr, endStr),
                fetchChartData(startStr, endStr)
            ]);
        } catch (error) {
            console.error("Error loading analytics", error);
        } finally {
            setLoading(false);
        }
    };

    const fetchUsageStats = async (startDate: string, endDate: string) => {
        try {
            const response = await analyticsService.getDailyUsage(startDate, endDate);
            setUsageStats(response.data);
        } catch (e) {
            console.error("Failed to fetch usage stats", e);
        }
    };

    const fetchQualityStats = async (startDate: string, endDate: string) => {
        try {
            const response = await analyticsService.getQualityTrend(startDate, endDate);
            setQualityStats(response.data);
        } catch (e) {
            console.error("Failed to fetch quality stats", e);
        }
    };

    const fetchQueryTypeStats = async (startDate: string, endDate: string) => {
        try {
            const response = await analyticsService.getQueryTypeStats(startDate, endDate);
            setQueryTypeStats(response.data);
        } catch (e) {
            console.error("Failed to fetch query type stats", e);
        }
    };

    const fetchChartData = async (startDate: string, endDate: string) => {
        try {
            const response = await analyticsService.getInteractionTimeSeries(startDate, endDate);
            const timeSeries = response.data;

            // Transform API data to chart format
            const labels = timeSeries.dataPoints.map(point => {
                const date = new Date(point.date);
                return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
            });
            const dataPoints = timeSeries.dataPoints.map(point => point.value);

            setChartData({
                labels,
                datasets: [
                    {
                        label: 'Interactions',
                        data: dataPoints,
                        borderColor: '#2563EB',
                        backgroundColor: (context: ScriptableContext<'line'>) => {
                            const ctx = context.chart.ctx;
                            const gradient = ctx.createLinearGradient(0, 0, 0, 300);
                            gradient.addColorStop(0, 'rgba(37, 99, 235, 0.1)');
                            gradient.addColorStop(1, 'rgba(37, 99, 235, 0)');
                            return gradient;
                        },
                        borderWidth: 3,
                        tension: 0.4,
                        fill: true,
                        pointRadius: 0,
                        pointHoverRadius: 6,
                    },
                ],
            });
        } catch (e) {
            console.error("Failed to fetch chart data", e);
            // Set empty chart data on error
            setChartData({
                labels: [],
                datasets: []
            });
        }
    };

    const chartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { display: false },
            tooltip: {
                mode: 'index' as const,
                intersect: false,
                backgroundColor: '#1F2937',
                titleColor: '#F9FAFB',
                bodyColor: '#F9FAFB',
                padding: 10,
                cornerRadius: 4,
                displayColors: false,
            },
        },
        scales: {
            y: {
                beginAtZero: true,
                grid: {
                    borderDash: [4, 4],
                    color: '#E5E7EB',
                    drawBorder: false,
                },
                ticks: { color: '#9CA3AF', font: { size: 11 } },
            },
            x: {
                grid: { display: false },
                ticks: { color: '#9CA3AF', font: { size: 11 } },
            },
        },
        interaction: {
            mode: 'nearest' as const,
            axis: 'x' as const,
            intersect: false,
        },
    };

    return (
        <div className="animate-fade-in">
            {/* Page Header */}
            <div className="mb-8">
                <h1 className="mb-2">Chat Analytics</h1>
                <p className="text-secondary">Analyze the performance of your chatbot interactions with detailed metrics.</p>
            </div>

            {/* Filters */}
            <div className="flex justify-between items-center mb-6">
                <div className="input-group mb-0">
                    <input
                        type="text"
                        className="form-control"
                        placeholder="Search interactions..."
                        style={{ width: '300px' }}
                    />
                </div>
                <div className="flex gap-sm">
                    <select
                        className="form-control"
                        style={{ width: 'auto' }}
                        value={dateRange}
                        onChange={(e) => setDateRange(e.target.value)}
                    >
                        <option value="7days">Last 7 Days</option>
                        <option value="30days">Last 30 Days</option>
                        <option value="month">This Month</option>
                    </select>
                    <select className="form-control" style={{ width: 'auto' }}>
                        <option>All Statuses</option>
                        <option>Active</option>
                        <option>Resolved</option>
                    </select>
                    <button className="btn btn-secondary" onClick={loadAnalytics}>
                        Refresh
                    </button>
                </div>
            </div>

            <h3 className="mb-4">Overall Performance</h3>

            {/* KPI Cards */}
            <div className="grid grid-cols-3 gap-lg mb-8">
                <div className="card">
                    <div className="text-sm text-secondary font-medium mb-2">Total Interactions</div>
                    <div className="text-4xl font-bold text-main mb-2">
                        {loading ? '-' : usageStats.totalInteractions.toLocaleString()}
                    </div>
                    <div className="flex items-center text-sm font-medium text-secondary">
                        <span>-</span>
                    </div>
                </div>
                <div className="card">
                    <div className="text-sm text-secondary font-medium mb-2">Avg Response Time</div>
                    <div className="text-4xl font-bold text-main mb-2">
                        {loading ? '-' : Math.round(usageStats.averageResponseTime)}
                        <span className="text-xl text-secondary font-normal ml-1">ms</span>
                    </div>
                    <div className="flex items-center text-sm font-medium text-secondary">
                        <span>-</span>
                    </div>
                </div>
                <div className="card">
                    <div className="text-sm text-secondary font-medium mb-2">User Satisfaction</div>
                    <div className="text-4xl font-bold text-main mb-2">
                        {loading ? '-' : qualityStats.averageRating.toFixed(2)}
                    </div>
                    <div className="flex items-center text-sm font-medium text-secondary">
                        <span>{loading ? '-' : `${qualityStats.helpfulPercentage.toFixed(1)}%`}</span>
                        <span className="ml-1 text-secondary font-normal">helpful</span>
                    </div>
                </div>
            </div>

            <h3 className="mb-4">Interaction Volume Over Time</h3>

            {/* Chart Section */}
            <div className="card mb-8 p-6">
                <div style={{ height: '300px', width: '100%' }}>
                    <Line options={chartOptions} data={chartData} />
                </div>
            </div>

            <h3 className="mb-4">Query Type Details</h3>

            {/* Table Section */}
            <div className="card p-0 overflow-hidden">
                <div className="table-container">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>QUERY TYPE</th>
                                <th>COUNT</th>
                                <th>AVG TIME</th>
                                <th>SATISFACTION</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr>
                                    <td colSpan={4} className="text-center p-4 text-muted">Loading data...</td>
                                </tr>
                            ) : queryTypeStats.length === 0 ? (
                                <tr>
                                    <td colSpan={4} className="text-center p-4 text-muted">No data available</td>
                                </tr>
                            ) : (
                                queryTypeStats.map((item, index) => (
                                    <tr key={index}>
                                        <td className="font-medium text-main">{item.queryType}</td>
                                        <td>{item.count.toLocaleString()}</td>
                                        <td>{Math.round(item.averageResponseTime)}ms</td>
                                        <td>
                                            <div className="flex items-center gap-sm">
                                                <div style={{ width: '60px', height: '6px', background: '#E5E7EB', borderRadius: '3px', overflow: 'hidden' }}>
                                                    <div style={{ width: `${item.averageRating * 20}%`, height: '100%', background: 'var(--primary)' }}></div>
                                                </div>
                                                <span className="text-xs text-secondary">{item.averageRating.toFixed(1)}</span>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default AnalyticsPage;
