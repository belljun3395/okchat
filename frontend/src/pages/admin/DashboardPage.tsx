import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { knowledgeBaseService, CreateKnowledgeBasePayload } from '../../services/knowledgeBase.service';
import { KnowledgeBase } from '../../types';
import { KnowledgeBaseModal } from '../../components/knowledge-base/CreateKnowledgeBaseModal';

const DashboardPage: React.FC = () => {
    const [kbs, setKbs] = useState<KnowledgeBase[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    
    // Modal State
    const [showModal, setShowModal] = useState(false);
    const [selectedKb, setSelectedKb] = useState<KnowledgeBase | undefined>(undefined);
    const [submitting, setSubmitting] = useState(false);

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

    const handleCreateOrUpdate = async (data: CreateKnowledgeBasePayload) => {
        setSubmitting(true);
        try {
            if (selectedKb) {
                await knowledgeBaseService.update(selectedKb.id, data);
            } else {
                await knowledgeBaseService.create(data);
            }
            setShowModal(false);
            setSelectedKb(undefined);
            fetchKbs(); // Refresh list
        } catch (err) {
            console.error('Failed to save KB:', err);
            alert('Failed to save Knowledge Base. ' + (err instanceof Error ? err.message : ''));
        } finally {
            setSubmitting(false);
        }
    };

    const openCreateModal = () => {
        setSelectedKb(undefined);
        setShowModal(true);
    };

    const openEditModal = async (kb: KnowledgeBase) => {
        try {
            // Fetch strict data (including config) from backend
            const detailedKb = await knowledgeBaseService.getById(kb.id);
            setSelectedKb(detailedKb);
            setShowModal(true);
        } catch (err) {
            console.error(err);
            alert("Failed to load Knowledge Base details.");
        }
    };

    return (
        <div className="animate-fade-in">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="mb-2">Admin Dashboard</h1>
                    <p className="text-secondary">Manage Knowledge Bases and Users</p>
                </div>
                <div className="flex gap-4">
                    <Link to="/admin/users" className="btn btn-secondary">
                        Manage Users
                    </Link>
                    <button 
                        className="btn btn-primary"
                        onClick={openCreateModal}
                    >
                        + Create Knowledge Base
                    </button>
                </div>
            </div>

            <div className="card p-0 overflow-hidden">
                <div className="table-container">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Type</th>
                                <th>Description</th>
                                <th>Creator</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan={6} className="text-center p-8">Loading...</td></tr>
                            ) : error ? (
                                <tr><td colSpan={6} className="text-center p-8 text-danger">{error}</td></tr>
                            ) : kbs.length === 0 ? (
                                <tr><td colSpan={6} className="text-center p-8">No Knowledge Bases found. Create one to get started.</td></tr>
                            ) : (
                                kbs.map(kb => (
                                    <tr key={kb.id}>
                                        <td className="font-medium text-main">
                                            <Link to={`/admin/knowledge-bases/${kb.id}`} className="hover:underline">
                                                {kb.name}
                                            </Link>
                                        </td>
                                        <td>
                                            <span className="badge badge-info">{kb.type}</span>
                                        </td>
                                        <td className="text-muted text-sm">{kb.description || '-'}</td>
                                        <td className="text-sm">ID: {kb.createdBy}</td>
                                        <td>
                                            {kb.enabled ? (
                                                <span className="badge badge-success">Active</span>
                                            ) : (
                                                <span className="badge badge-secondary">Inactive</span>
                                            )}
                                        </td>
                                        <td>
                                            <div className="flex gap-2">
                                                <Link 
                                                    to={`/admin/knowledge-bases/${kb.id}`}
                                                    className="btn btn-sm btn-secondary"
                                                >
                                                    Manage
                                                </Link>
                                                <button
                                                    className="btn btn-sm btn-outline"
                                                    onClick={() => openEditModal(kb)}
                                                >
                                                    Edit
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Create/Edit Component Modal */}
            {showModal && (
                <KnowledgeBaseModal 
                    onClose={() => setShowModal(false)}
                    onSubmit={handleCreateOrUpdate}
                    loading={submitting}
                    initialData={selectedKb}
                />
            )}
        </div>
    );
};

export default DashboardPage;
