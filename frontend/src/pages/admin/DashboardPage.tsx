import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { knowledgeBaseService, CreateKnowledgeBasePayload } from '../../services/knowledgeBase.service'; // Ensure imports
import { KnowledgeBase } from '../../types';

const DashboardPage: React.FC = () => {
    const [kbs, setKbs] = useState<KnowledgeBase[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    
    // Modal State
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [newKbData, setNewKbData] = useState<CreateKnowledgeBasePayload>({
        name: '',
        description: '',
        type: 'CONFLUENCE', // Default
        config: {}
    });
    const [creating, setCreating] = useState(false);

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

    const handleCreate = async (e: React.FormEvent) => {
        e.preventDefault();
        setCreating(true);
        try {
            await knowledgeBaseService.create(newKbData);
            setShowCreateModal(false);
            setNewKbData({ name: '', description: '', type: 'CONFLUENCE', config: {} });
            fetchKbs(); // Refresh list
        } catch (err) {
            console.error('Failed to create KB:', err);
            alert('Failed to create Knowledge Base');
        } finally {
            setCreating(false);
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
                        onClick={() => setShowCreateModal(true)}
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
                                            <Link 
                                                to={`/admin/knowledge-bases/${kb.id}`}
                                                className="btn btn-sm btn-secondary"
                                            >
                                                Manage
                                            </Link>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Create Component Modal - Simplified Inline */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fade-in">
                    <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
                        <h2 className="text-xl font-bold mb-4">Create Knowledge Base</h2>
                        <form onSubmit={handleCreate}>
                            <div className="mb-4">
                                <label className="block text-sm font-medium mb-1">Name</label>
                                <input 
                                    type="text" 
                                    className="form-control w-full"
                                    value={newKbData.name}
                                    onChange={e => setNewKbData({...newKbData, name: e.target.value})}
                                    required
                                />
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium mb-1">Description</label>
                                <textarea 
                                    className="form-control w-full"
                                    value={newKbData.description}
                                    onChange={e => setNewKbData({...newKbData, description: e.target.value})}
                                />
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium mb-1">Type</label>
                                <select 
                                    className="form-control w-full"
                                    value={newKbData.type}
                                    onChange={e => setNewKbData({...newKbData, type: e.target.value})}
                                >
                                    <option value="CONFLUENCE">Confluence</option>
                                    <option value="ETC">ETC</option>
                                </select>
                            </div>
                            <div className="flex justify-end gap-2 mt-6">
                                <button 
                                    type="button" 
                                    className="btn btn-secondary"
                                    onClick={() => setShowCreateModal(false)}
                                    disabled={creating}
                                >
                                    Cancel
                                </button>
                                <button 
                                    type="submit" 
                                    className="btn btn-primary"
                                    disabled={creating}
                                >
                                    {creating ? 'Creating...' : 'Create'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default DashboardPage;
