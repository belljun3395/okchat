import React from 'react';
import { Link, Outlet, useLocation, useParams } from 'react-router-dom';

const KnowledgeBaseLayout: React.FC = () => {
    const location = useLocation();
    const { id } = useParams<{ id: string }>();

    const isActive = (path: string) => location.pathname === path || location.pathname.startsWith(path + '/');
    const baseUrl = `/admin/knowledge-bases/${id}`;

    return (
        <div className="admin-layout">
            <aside className="admin-sidebar">
                <div className="admin-sidebar-header">
                    <Link to="/admin" className="nav-brand">
                        &larr; Back to Admin
                    </Link>
                </div>



                <nav className="admin-sidebar-nav px-2">
                    <Link to={`${baseUrl}/members`} className={`nav-item px-5 ${isActive(`${baseUrl}/members`) ? 'active' : ''}`}>
                        Members
                    </Link>

                    <Link to={`${baseUrl}/paths`} className={`nav-item px-5 ${isActive(`${baseUrl}/paths`) ? 'active' : ''}`}>
                        Paths
                    </Link>

                    <Link to={`${baseUrl}/permissions`} className={`nav-item px-5 ${isActive(`${baseUrl}/permissions`) ? 'active' : ''}`}>
                        Permissions
                    </Link>

                    <Link to={`${baseUrl}/email`} className={`nav-item px-5 ${isActive(`${baseUrl}/email`) ? 'active' : ''}`}>
                        Email Review
                    </Link>
                </nav>
            </aside>

            <main className="admin-main">
                <Outlet />
            </main>
        </div>
    );
};

export default KnowledgeBaseLayout;
