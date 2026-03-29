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
import SavedFarms from './pages/SavedFarms';

//guard wrapper that redirects to login if not authenticated,
//or sends you to the right dashboard if your role doesn't match
function ProtectedRoute({ children, allowedRole }) {
    const { user } = useAuth();

    if (!user) {
        return <Navigate to="/login" />;
    }

    if (allowedRole && user.role !== allowedRole) {
        return <Navigate to={user.role === 'BUYER' ? '/buyer/profile' : '/seller/dashboard'} />;
    }

    return children;
}

//all the route definitions live here, grouped by auth pages,
//buyer pages, seller pages, and some legacy redirects at the bottom
function AppRoutes() {
    const { user } = useAuth();

    return (
        <Routes>
            {/* Auth */}
            <Route path="/login" element={
                user ? <Navigate to={user.role === 'BUYER' ? '/buyer/profile' : '/seller/dashboard'} /> : <Login />
            } />
            <Route path="/register/buyer"  element={user ? <Navigate to="/buyer/profile-setup" /> : <BuyerSignup />} />
            <Route path="/register/seller" element={user ? <Navigate to="/seller/profile-setup" /> : <SellerSignup />} />

            {/* Farm listings — accessible buyers */}
            <Route path="/buyer/farmlistings" element={
                <ProtectedRoute allowedRole="BUYER">
                    <FarmListingsPage />
                </ProtectedRoute>
            } />
            <Route path="/buyer/farmlistings/:farmname" element={
                <ProtectedRoute allowedRole="BUYER">
                    <FarmListing />
                </ProtectedRoute>
            } />
			
			<Route path="/buyer/saved_farms" element={
				<ProtectedRoute allowedRole="BUYER">
					<SavedFarms />
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
            <Route path="/farmlistingspage"        element={<Navigate to="/buyer/farmlistings"   replace />} />
            <Route path="/farmlistingspage/:farmname" element={<Navigate to="/buyer/farmlistings/:farmname" replace />} />

            {/* Fallback */}
            <Route path="/" element={<Navigate to="/login" />} />
            <Route path="*" element={<Navigate to="/login" />} />
        </Routes>
    );
}

//top-level component, just wires up the router and auth provider
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
