import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { userService, permissionService } from '../../services';

const DashboardPage: React.FC = () => {
    const [stats, setStats] = useState({
        userCount: 0,
        pathCount: 0,
        loading: true
    });

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const [usersRes, pathsRes] = await Promise.all([
                    userService.getAll(),
                    permissionService.getAllPaths()
                ]);

                setStats({
                    userCount: usersRes.data.length,
                    pathCount: pathsRes.data.length,
                    loading: false
                });
            } catch (error) {
                console.error('Failed to fetch dashboard stats:', error);
                setStats(prev => ({ ...prev, loading: false }));
            }
        };

        fetchStats();
    }, []);

    return (
        <div className="animate-fade-in">
            <div className="mb-8">
                <h1 className="mb-2">Dashboard</h1>
                <p className="text-secondary">Overview of system permissions and user activity</p>
            </div>

            {/* Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-lg mb-8">
                <div className="card p-6">
                    <p className="text-sm font-medium text-secondary mb-1">Total Users</p>
                    <h3 className="text-3xl font-bold text-main mb-1">
                        {stats.loading ? '-' : stats.userCount}
                    </h3>
                    <p className="text-xs text-muted">Registered accounts</p>
                </div>

                <div className="card p-6">
                    <p className="text-sm font-medium text-secondary mb-1">Document Paths</p>
                    <h3 className="text-3xl font-bold text-main mb-1">
                        {stats.loading ? '-' : stats.pathCount}
                    </h3>
                    <p className="text-xs text-muted">Secured directories</p>
                </div>
            </div>

            {/* Quick Actions */}
            <h3 className="mb-4 text-lg font-semibold">Quick Actions</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-lg">
                <Link to="/" className="card p-6 hover:shadow-md transition-all">
                    <h4 className="mb-2">Chat Interface</h4>
                    <p className="text-sm text-secondary mb-4">Access the AI-powered search and Q&A interface</p>
                    <div className="text-sm text-primary font-medium">
                        Go to Chat →
                    </div>
                </Link>

                <Link to="/admin/permissions/manage" className="card p-6 hover:shadow-md transition-all">
                    <h4 className="mb-2">Permission Manager</h4>
                    <p className="text-sm text-secondary mb-4">Configure global access policies and inheritance</p>
                    <div className="text-sm text-primary font-medium">
                        Manage Permissions →
                    </div>
                </Link>

                <Link to="/admin/users" className="card p-6 hover:shadow-md transition-all">
                    <h4 className="mb-2">Manage Users</h4>
                    <p className="text-sm text-secondary mb-4">View user list and manage access details</p>
                    <div className="text-sm text-primary font-medium">
                        View Users →
                    </div>
                </Link>
            </div>
        </div>
    );
};

export default DashboardPage;
