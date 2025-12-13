import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { knowledgeBaseService } from '../../services/knowledgeBase.service'; // Ensure this export exists

import type { KnowledgeBase } from '../../types';

const KnowledgeBaseListPage: React.FC = () => {
    const [kbs, setKbs] = useState<KnowledgeBase[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        fetchKbs();
    }, []);

    const fetchKbs = async () => {
        try {
            setLoading(true);
            const response = await knowledgeBaseService.getAll();
            setKbs(response.data);
            setError('');
        } catch (err) {
            console.error('Failed to fetch KBs:', err);
            setError('Failed to load Knowledge Bases.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="animate-fade-in">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="mb-2">Knowledge Bases</h1>
                    <p className="text-secondary">Manage Knowledge Bases and memberships</p>
                </div>
            </div>

            <div className="card p-0 overflow-hidden">
                <div className="table-container">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Type</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan={5} className="text-center p-8">Loading...</td></tr>
                            ) : error ? (
                                <tr><td colSpan={5} className="text-center p-8 text-danger">{error}</td></tr>
                            ) : kbs.length === 0 ? (
                                <tr><td colSpan={5} className="text-center p-8">No Knowledge Bases found</td></tr>
                            ) : (
                                kbs.map(kb => (
                                    <tr key={kb.id}>
                                        <td className="font-mono text-secondary">{kb.id}</td>
                                        <td className="font-medium text-main">{kb.name}</td>
                                        <td>
                                            <span className="badge badge-info">{kb.type}</span>
                                        </td>
                                        <td>
                                            {kb.enabled ? (
                                                <span className="badge badge-success">Active</span>
                                            ) : (
                                                <span className="badge badge-secondary">Inactive</span>
                                            )}
                                        </td>
                                        <td>
                                            <Link 
                                                to={`/admin/knowledge-bases/${kb.id}/members`}
                                                className="text-primary hover:underline font-medium text-sm"
                                            >
                                                Manage Members
                                            </Link>
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

export default KnowledgeBaseListPage;
