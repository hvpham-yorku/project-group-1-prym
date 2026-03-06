import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/auth/Login';
import BuyerSignup from './pages/auth/BuyerSignup';
import SellerSignup from './pages/auth/SellerSignup';
import BuyerDashboard from './pages/BuyerDashboard';
import BuyerProfileSetup from './pages/BuyerProfileSetup';
import SellerDashboard from './pages/SellerDashboard';
import FarmListingsPage from './pages/FarmListingsPage';
import FarmListing from './pages/FarmListing';
import SellerProfileSetup from './pages/SellerProfileSetup';
import GroupDetailPage from './pages/GroupDetailPage';
import CreateGroupPage from './pages/CreateGroupPage';
import BrowseGroupsPage from './pages/BrowseGroupsPage';

function ProtectedRoute({ children, allowedRole }) {
    const { user } = useAuth();

    if (!user) {
        return <Navigate to="/login" />;
    }

    if (allowedRole && user.role !== allowedRole) {
        return <Navigate to={user.role === 'BUYER' ? '/farmlistings' : '/seller/dashboard'} />;
    }

    return children;
}

function AppRoutes() {
    const { user } = useAuth();

    return (
        <Routes>
            {/* Auth */}
            <Route path="/login" element={
                user ? <Navigate to={user.role === 'BUYER' ? '/farmlistings' : '/seller/dashboard'} /> : <Login />
            } />
            <Route path="/register/buyer"  element={user ? <Navigate to="/buyer/profile-setup" /> : <BuyerSignup />} />
            <Route path="/register/seller" element={user ? <Navigate to="/seller/profile-setup" /> : <SellerSignup />} />

            {/* Farm listings — accessible to any logged-in user */}
            <Route path="/farmlistings" element={
                <ProtectedRoute>
                    <FarmListingsPage />
                </ProtectedRoute>
            } />
            <Route path="/farmlistings/:farmid" element={
                <ProtectedRoute>
                    <FarmListing />
                </ProtectedRoute>
            } />

            {/* Buyer */}
            <Route path="/buyer/profile" element={
                <ProtectedRoute allowedRole="BUYER">
                    <BuyerDashboard />
                </ProtectedRoute>
            } />
            <Route path="/buyer/profile-setup" element={
                <ProtectedRoute allowedRole="BUYER">
                    <BuyerProfileSetup />
                </ProtectedRoute>
            } />

            <Route path="/buyer/groups/:groupId" element={
                <ProtectedRoute allowedRole="BUYER">
                    <GroupDetailPage />
                </ProtectedRoute>
            } />
            <Route path="/buyer/create-group" element={
                <ProtectedRoute allowedRole="BUYER">
                    <CreateGroupPage />
                </ProtectedRoute>
            } />
            <Route path="/buyer/browse-groups" element={
                <ProtectedRoute allowedRole="BUYER">
                    <BrowseGroupsPage />
                </ProtectedRoute>
            } />

            {/* Seller */}
            <Route path="/seller/dashboard" element={
                <ProtectedRoute allowedRole="SELLER">
                    <SellerDashboard />
                </ProtectedRoute>
            } />
            <Route path="/seller/profile-setup" element={
                <ProtectedRoute allowedRole="SELLER">
                    <SellerProfileSetup />
                </ProtectedRoute>
            } />

            {/* Legacy redirects — keep old URLs working */}
            <Route path="/buyer/dashboard"        element={<Navigate to="/buyer/profile"  replace />} />
            <Route path="/farmlistingspage"        element={<Navigate to="/farmlistings"   replace />} />
            <Route path="/farmlistingspage/:farmid" element={<Navigate to="/farmlistings/:farmid" replace />} />

            {/* Fallback */}
            <Route path="/" element={<Navigate to="/login" />} />
            <Route path="*" element={<Navigate to="/login" />} />
        </Routes>
    );
}

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <AppRoutes />
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;
