import React, { useState } from 'react';
import { CreateKnowledgeBasePayload } from '../../services/knowledgeBase.service';
import { KnowledgeBase } from '../../types';

interface KnowledgeBaseModalProps {
    onClose: () => void;
    onSubmit: (data: CreateKnowledgeBasePayload) => Promise<void>;
    loading: boolean;
    initialData?: KnowledgeBase & { config?: Record<string, unknown> }; // Support config in initial data
}

type ProviderType = 'GMAIL' | 'OUTLOOK';

export const KnowledgeBaseModal: React.FC<KnowledgeBaseModalProps> = ({ onClose, onSubmit, loading, initialData }) => {
    const isEdit = !!initialData;

    // Parse email config from initialData if exists
    const config = initialData?.config as { emailProviders?: Record<string, unknown>; spaceKey?: string } | undefined;
    const initialEmailProviders = config?.emailProviders ? Object.values(config.emailProviders) : [];
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const firstProvider = initialEmailProviders.length > 0 ? initialEmailProviders[0] as any : null; // Using any strictly for legacy config extraction

    const [name, setName] = useState(initialData?.name || '');
    const [description, setDescription] = useState(initialData?.description || '');
    const [type, setType] = useState(initialData?.type || 'CONFLUENCE');

    // Confluence Config State
    const [spaceKey, setSpaceKey] = useState(config?.spaceKey || '');

    // Email Config State
    const [enableEmail, setEnableEmail] = useState(!!firstProvider);
    const [providerType, setProviderType] = useState<ProviderType>(firstProvider?.type || 'GMAIL');
    const [emailUsername, setEmailUsername] = useState(firstProvider?.username || '');
    const [clientId, setClientId] = useState(firstProvider?.oauth2?.clientId || '');
    const [clientSecret, setClientSecret] = useState(firstProvider?.oauth2?.clientSecret || '');
    const [tenantId, setTenantId] = useState(firstProvider?.oauth2?.tenantId || 'common');

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();

        const config: Record<string, unknown> = {};

        // Confluence config: spaceKey
        if (type === 'CONFLUENCE' && spaceKey) {
            config.spaceKey = spaceKey;
        }

        if (enableEmail) {
            const providerConfig = {
                type: providerType,
                host: providerType === 'GMAIL' ? 'imap.gmail.com' : 'outlook.office365.com',
                port: 993,
                username: emailUsername,
                authType: 'OAUTH2',
                enabled: true,
                oauth2: {
                    clientId,
                    clientSecret,
                    tenantId: providerType === 'OUTLOOK' ? tenantId : undefined,
                    scopes: providerType === 'GMAIL'
                        ? ['https://mail.google.com/']
                        : ['https://outlook.office365.com/IMAP.AccessAsUser.All', 'https://outlook.office365.com/SMTP.Send', 'offline_access'],
                    redirectUri: 'http://localhost:8080/oauth2/callback'
                }
            };

            config.emailProviders = {
                [providerType.toLowerCase()]: providerConfig
            };
        }

        onSubmit({
            name,
            description,
            type,
            config
        });
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 animate-fade-in" style={{ backdropFilter: "blur(4px)" }}>
            <div className="bg-white rounded-lg shadow-2xl overflow-hidden flex flex-col mx-4 w-full" style={{ maxWidth: "1000px", maxHeight: "90vh" }}>
                {/* Header */}
                <div className="px-8 py-6 border-b border-gray-200 flex justify-between items-center bg-white">
                    <div>
                        <h2 className="text-xl font-bold text-gray-900">{isEdit ? 'Edit Knowledge Base' : 'Create Knowledge Base'}</h2>
                        <p className="text-sm text-gray-500 mt-1">Configure your knowledge base settings and integrations</p>
                    </div>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 p-2 hover:bg-gray-100 rounded-lg transition-colors">
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-8">
                    <form id="kb-form" onSubmit={handleSubmit} className="grid grid-cols-2 gap-8">
                        {/* Left Column: Basic Info */}
                        <div className="space-y-6">
                            <div className="flex items-center gap-3 mb-6">
                                <span className="rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center w-10 h-10 border border-blue-100">
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                </span>
                                <div>
                                    <h3 className="text-lg font-semibold text-gray-900">Basic Information</h3>
                                    <p className="text-sm text-gray-500">General settings for this knowledge base</p>
                                </div>
                            </div>

                            <div className="bg-gray-50 border border-gray-200 rounded-xl p-6 space-y-5">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1.5">Name <span className="text-danger">*</span></label>
                                    <input
                                        type="text"
                                        className="form-control"
                                        value={name}
                                        onChange={e => setName(e.target.value)}
                                        placeholder="e.g., Engineering Docs"
                                        required
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1.5">Description</label>
                                    <textarea
                                        className="form-control"
                                        style={{ minHeight: "120px" }}
                                        value={description}
                                        onChange={e => setDescription(e.target.value)}
                                        placeholder="Describe the purpose of this knowledge base..."
                                        rows={4}
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1.5">Type <span className="text-danger">*</span></label>
                                    <div className="relative">
                                        <select
                                            className="form-control"
                                            value={type}
                                            onChange={e => setType(e.target.value)}
                                        >
                                            <option value="CONFLUENCE">Confluence</option>
                                            <option value="ETC">ETC</option>
                                        </select>
                                    </div>
                                </div>
                                {type === 'CONFLUENCE' && (
                                    <div className="animate-fade-in">
                                        <label className="block text-sm font-medium text-gray-700 mb-1.5">Space Key <span className="text-danger">*</span></label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            value={spaceKey}
                                            onChange={e => setSpaceKey(e.target.value)}
                                            placeholder="e.g., ENGINEERING"
                                            required
                                        />
                                        <p className="text-xs text-gray-500 mt-1">Confluence Space Key to sync documents from</p>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Right Column: Integration */}
                        <div className="space-y-6">
                            <div className="flex items-center justify-between mb-6">
                                <div className="flex items-center gap-3">
                                    <span className="rounded-lg flex items-center justify-center w-10 h-10 border" style={{ backgroundColor: "#faf5ff", borderColor: "#f3e8ff", color: "#9333ea" }}>
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path></svg>
                                    </span>
                                    <div>
                                        <h3 className="text-lg font-semibold text-gray-900">Email Integration</h3>
                                        <p className="text-sm text-gray-500">Configure email source for this KB</p>
                                    </div>
                                </div>
                                <div className="flex items-center">
                                    <label className="inline-flex items-center cursor-pointer select-none gap-2 bg-gray-50 px-3 py-1.5 rounded-full border border-gray-200 hover:bg-gray-100 transition-colors">
                                        <input
                                            type="checkbox"
                                            checked={enableEmail}
                                            onChange={e => setEnableEmail(e.target.checked)}
                                            className="cursor-pointer m-0"
                                            style={{ width: "16px", height: "16px", marginTop: "2px" }}
                                        />
                                        <span className={`text-sm font-medium ${enableEmail ? 'text-gray-900' : 'text-gray-500'}`}>{enableEmail ? 'Enabled' : 'Disabled'}</span>
                                    </label>
                                </div>
                            </div>

                            <div className={`transition-all duration-300 ${enableEmail ? 'opacity-100' : 'opacity-40 grayscale pointer-events-none'}`}>
                                <div className="bg-gray-50 border border-gray-200 rounded-xl p-6 space-y-5">
                                    <div>
                                        <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Provider</label>
                                        <div className="grid grid-cols-2 gap-4">
                                            <label
                                                className={`cursor-pointer border rounded-lg p-3 flex items-center justify-center gap-3 transition-all ${providerType === 'GMAIL'
                                                        ? 'border-blue-500 bg-blue-50 text-blue-700 shadow-sm ring-1 ring-blue-500'
                                                        : 'border-gray-200 hover:bg-white bg-white text-gray-600 hover:border-gray-300'
                                                    }`}
                                            >
                                                <input
                                                    type="radio"
                                                    className="sr-only"
                                                    name="provider"
                                                    value="GMAIL"
                                                    checked={providerType === 'GMAIL'}
                                                    onChange={() => setProviderType('GMAIL')}
                                                />
                                                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                                                    <path d="M24 5.457v13.909c0 .904-.732 1.636-1.636 1.636h-3.819V11.73L12 16.64l-6.545-4.91v9.273H1.636A1.636 1.636 0 0 1 0 19.366V5.457c0-2.023 2.309-3.178 3.927-1.964L5.455 4.64 12 9.548l6.545-4.91 1.528-1.145C21.69 2.28 24 3.434 24 5.457z" />
                                                </svg>
                                                <span className="font-medium">Gmail</span>
                                            </label>
                                            <label
                                                className={`cursor-pointer border rounded-lg p-3 flex items-center justify-center gap-3 transition-all ${providerType === 'OUTLOOK'
                                                        ? 'border-blue-500 bg-blue-50 text-blue-700 shadow-sm ring-1 ring-blue-500'
                                                        : 'border-gray-200 hover:bg-white bg-white text-gray-600 hover:border-gray-300'
                                                    }`}
                                            >
                                                <input
                                                    type="radio"
                                                    className="sr-only"
                                                    name="provider"
                                                    value="OUTLOOK"
                                                    checked={providerType === 'OUTLOOK'}
                                                    onChange={() => setProviderType('OUTLOOK')}
                                                />
                                                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                                                    <path d="M1.636 4.909A1.636 1.636 0 0 1 3.273 3.273h5.454v5.455H1.636V4.91zM10.364 3.273H22.36c.904 0 1.637.732 1.637 1.636v5.455h-13.637V3.273zM24 11.727v7.364c0 .904-.732 1.636-1.636 1.636H10.364v-9H24zM1.636 10.364h7.09v9H3.273a1.636 1.636 0 0 1-1.637-1.637v-7.363z" fill="#0078d4" />
                                                </svg>
                                                <span className="font-medium">Outlook</span>
                                            </label>
                                        </div>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-1.5">Email Address</label>
                                        <input
                                            type="email"
                                            className="form-control"
                                            value={emailUsername}
                                            onChange={e => setEmailUsername(e.target.value)}
                                            placeholder={providerType === 'GMAIL' ? 'user@gmail.com' : 'user@outlook.com'}
                                            required={enableEmail}
                                        />
                                    </div>

                                    <div className="space-y-4">
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-1.5">Client ID</label>
                                            <input
                                                type="text"
                                                className="form-control bg-white"
                                                style={{ fontFamily: "monospace", fontSize: "13px" }}
                                                value={clientId}
                                                onChange={e => setClientId(e.target.value)}
                                                placeholder="OAuth2 Client ID"
                                                required={enableEmail}
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-1.5">Client Secret</label>
                                            <input
                                                type="password"
                                                className="form-control bg-white"
                                                style={{ fontFamily: "monospace", fontSize: "13px" }}
                                                value={clientSecret}
                                                onChange={e => setClientSecret(e.target.value)}
                                                placeholder={isEdit && enableEmail ? "••••••••" : "OAuth2 Client Secret"}
                                                required={enableEmail && (!isEdit || !clientSecret)}
                                            />
                                        </div>
                                    </div>

                                    {providerType === 'OUTLOOK' && (
                                        <div className="animate-fade-in">
                                            <label className="block text-sm font-medium text-gray-700 mb-1.5">Tenant ID</label>
                                            <input
                                                type="text"
                                                className="form-control bg-white"
                                                style={{ fontFamily: "monospace", fontSize: "13px" }}
                                                value={tenantId}
                                                onChange={e => setTenantId(e.target.value)}
                                                placeholder="common"
                                                required={enableEmail}
                                            />
                                        </div>
                                    )}

                                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-xs text-blue-700 flex gap-2 items-start">
                                        <svg className="w-4 h-4 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                        <p>OAuth Redirect URI: <code className="bg-white px-1.5 py-0.5 rounded border border-blue-200 font-mono select-all">http://localhost:8080/oauth2/callback</code></p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </form>
                </div>

                {/* Footer */}
                <div className="px-8 py-6 border-t border-gray-200 bg-white flex justify-end gap-3">
                    <button
                        type="button"
                        className="btn btn-secondary bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 shadow-sm"
                        onClick={onClose}
                        disabled={loading}
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        form="kb-form"
                        className="btn btn-primary flex items-center gap-2 shadow-sm"
                        disabled={loading}
                    >
                        {loading && <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>}
                        {isEdit ? 'Save Changes' : 'Create Knowledge Base'}
                    </button>
                </div>
            </div>
        </div>
    );
};
