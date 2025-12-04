import React from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';

/**
 * AdminLayout Component
 * 
 * This component provides the shared structure for all admin pages.
 * It includes the sidebar navigation and the main content area.
 * The `Outlet` component from react-router-dom renders the child route's component.
 */
const AdminLayout: React.FC = () => {
    const location = useLocation();

    // Helper function to check if a link is active
    const isActive = (path: string) => location.pathname === path;

    return (
        <div className="admin-layout">
            {/* Sidebar Navigation */}
            <aside className="admin-sidebar">
                <div className="admin-sidebar-header">
                    <Link to="/admin/permissions" className="nav-brand">
                        OKChat Admin
                    </Link>
                </div>

                <nav className="admin-sidebar-nav">
                    <Link to="/admin/permissions" className={`nav-item ${isActive('/admin/permissions') ? 'active' : ''}`}>
                        Dashboard
                    </Link>

                    <Link to="/admin/users" className={`nav-item ${isActive('/admin/users') ? 'active' : ''}`}>
                        Users
                    </Link>

                    <Link to="/admin/paths" className={`nav-item ${isActive('/admin/paths') ? 'active' : ''}`}>
                        Paths
                    </Link>

                    <Link to="/admin/permissions/manage" className={`nav-item ${isActive('/admin/permissions/manage') ? 'active' : ''}`}>
                        Permissions
                    </Link>

                    <Link to="/admin/analytics" className={`nav-item ${isActive('/admin/analytics') ? 'active' : ''}`}>
                        Analytics
                    </Link>

                    <Link to="/admin/email/review" className={`nav-item ${isActive('/admin/email/review') ? 'active' : ''}`}>
                        Email Review
                    </Link>
                </nav>
            </aside>

            {/* Main Content Area */}
            <main className="admin-main">
                {/* 
                    Outlet renders the component for the current route.
                    For example, if the URL is /admin/users, the UserManagementPage will be rendered here.
                */}
                <Outlet />
            </main>
        </div>
    );
};

export default AdminLayout;
