import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { logout } from '../api/auth';
import { useState, useEffect } from 'react';
import { getSellerProfile } from '../api/seller';
import { updateSellerProfile } from '../api/seller';

function SellerDashboard() {
    const { user, clearUser } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        try {
            await logout();
        } catch (error) {
            console.error('Logout failed:', error);
        } finally {
            clearUser();
            navigate('/login');
        }
    };
	
	const[isEditing, setIsEditing] = useState(false); 
	
	const[formData, setFormData] = useState({
		firstName: user?.firstName || '',
		lastName: user?.lastName || '',
		userName: user?.username || '',
		email: user?.email || '',
		phoneNumber: user?.phoneNumber || ''
	})
	
	const[activeSection, setActiveSection] = useState("Dashboard");
	
	const[profile, setProfile] = useState(null);
	const[loading, setLoading] = useState(true);
	const[error, setError] = useState('');
	
	useEffect(()=>{
		const fetchProfile = async() =>{
			try{
				setLoading(true);
				const data = await getSellerProfile(user.id);
				setProfile(data);
				setFormData({
					firstName: data.firstName || '',
					lastName: data.lastName ||'',
					username: data.username ||'',
					email: data.email ||'',
					phoneNumber: data.phoneNumber ||''
				});
			}
			catch (err){
				setError("Failed to load profile.");
				console.error(err);
			}
			finally{
				setLoading(false);
			}
		};
		fetchProfile();
	}, [user.id]);
	
	const handleSave = async () => {
	    try {
	        const payload = {
	            firstName: formData.firstName,
	            lastName: formData.lastName,
	            email: formData.email,
	            userName: formData.username,
	            phoneNumber: formData.phoneNumber
	        };
	        const updated = await updateSellerProfile(user.id, payload);
	        
	        //Update the local state
	        setProfile(updated);  
	        setIsEditing(false); 
	        window.location.reload(); 
	        
	    } catch (err) {
	        console.error("Save error:", err);
	        alert("Failed to save profile. Check the console for errors.");
	    }
	};

    return (
		<div>
			<Nav setActiveSection = {setActiveSection} handleLogout = {handleLogout} />
			
     	    <div style={styles.container}>
        	    <div style={styles.card}>
				
					{activeSection === "Dashboard" && (
						<>
							<h1 style = {styles.title}>Seller Dashboard</h1>
							<p style = {styles.welcome}>Welcome, {user?.username} !</p>
							<p style = {styles.info}>You are logged in as a <strong>SELLER</strong></p>
						</>	
					)}
				
				
            	    {activeSection === "Profile" && ( <>
                	<div style={styles.profilePictureContainer}>
                    	{user?.profilePicture ? (
                        	<img 
                            	src={user.profilePicture} 
                       	  	    alt="Profile" 
                        	    style={styles.profilePicture} 
      	                	  />
        	            ) : (
            	            <div style={styles.profilePicturePlaceholder}>
                	            {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
                    	    </div>
                    	)}
  	               </div>

    	            <h1 style={styles.title}>Your Profile</h1>
        	        <p style={styles.welcome}>
						Welcome, {isEditing ? formData.firstName:user?.username}!
					</p>
            	    <p style={styles.name}>
						{isEditing ? `${formData.firstName} ${formData.lastName}` : `${user?.firstName} ${user?.lastName}`}
					</p>
                	<p style={styles.info}>You are logged in as a <strong>SELLER</strong></p>
                
   	           		{/* User Info Card */}
    	            <div style={styles.infoCard}>
        	            <div style={styles.infoRow}>
							
							<span style={styles.infoLabel}>First Name:</span>
								{isEditing?(
									<input
										type = "text"
										value={formData.firstName}
										onChange={(e) => setFormData({...formData, firstName: e.target.value})}
									/>
								):(
									<span style = {styles.infoValue}>
										{user?.firstName}
									</span>
								)}
						</div>	
						
						<div style={styles.infoRow}>
						   <span style={styles.infoLabel}>Last Name:</span>
						   {isEditing ? (
						     <input
						       type="text"
						       value={formData.lastName}
						       onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
						     />
						   ) : (
						     <span style={styles.infoValue}>{user?.lastName}</span>
						   )}
						 </div>
						 
						 <div style={styles.infoRow}>
						     <span style={styles.infoLabel}>Username:</span>
						     {isEditing ? (
						       <input
						         type="text"
						         value={formData.username}
						         onChange={(e) => setFormData({ ...formData, username: e.target.value })}
						       />
						     ) : (
						       <span style={styles.infoValue}>{user?.username}</span>
						     )}
						   </div>
						   
						   <div style={styles.infoRow}>
						       <span style={styles.infoLabel}>Phone:</span>
						       {isEditing ? (
						         <input
						           type="tel"
						           value={formData.phoneNumber}
						           onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
						         />
						       ) : (
						         <span style={styles.infoValue}>{user?.phoneNumber}</span>
						       )}
						     </div>
							 
							 {/* Email - read only */}
							   <div style={styles.infoRow}>
							     <span style={styles.infoLabel}>Email:</span>
							     <span style={styles.infoValue}>{user?.email}</span>
							   </div>
						</div>
					
					<div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '20px' }}>
					
					{isEditing?(
						<>
							<button
								style ={styles.button}
								onClick={handleSave}>
								Save
							</button>
							
							<button
								style = {styles.button}
								onClick={()=>{
									setFormData({
										firstName:user?.firstName || '',
										lastName:user?.lastName || '',
										username:user?.username || '',
										email:user?.email || '',
										phoneNumber: user?.phoneNumber || ''
									});
									setIsEditing(false);
								}}>
								Discard
							</button>
						</>
					):(
						<>
							<button
								style={styles.button}
								onClick={() => setIsEditing(true)}>
								Edit Profile
							</button>
							<button
								style={styles.button}
								onClick={handleLogout}>
								Logout
							</button>
						</>								
					)}
				</div>
			</>
			)}
			</div>
        </div>
	</div>
    );
}

function Nav({setActiveSection, handleLogout} ){
	const [hamburgerOpen, setHamburgerOpen] = useState(false);
	const toggleHamburger = () => {
		setHamburgerOpen(!hamburgerOpen)
	}
	return (
		<div>
			<div style={{
				position: 'absolute',
				top:20,
				left:20,
				fontSize: '24px',
				cursor: 'pointer',
				zIndex: 1000
			}}
			onClick={toggleHamburger}>
				☰
			</div>
				
			{hamburgerOpen && (
				<div className = "sidebar" style={{
					width: '200px',
					height: '100vh',
					backgroundColor: '#ddd',
					position: 'fixed',
					top: 0,
					left: 0,
					padding: '20px',
					paddingTop: '70px',
					zIndex: 999
				}}>
					<ul style = {{ listStyle : 'none', padding: 0}}>
						<li>
							<button
								style={{
									background:'none',
									border:'none',
									cursor:'pointer',
									padding:0
								}}
								onClick={() => {
									setActiveSection("Dashboard");
									setHamburgerOpen(false);
								}}>
								Dashboard
							</button>
						</li>
						<li>
							<button
								style={{
									background: 'none',
									border : 'none',
									cursor : 'pointer',
									padding: 0
								}}
								onClick = {() => {
									setActiveSection("Profile");
									setHamburgerOpen(false);
								}}>
							Profile
							</button>
						</li>
						<li>
						<button
								style={{
									background: 'none',	
									border : 'none',
									cursor : 'pointer',
									padding: 0
								}}
								onClick = {() => {
									setActiveSection("Notifications");
									setHamburgerOpen(false);
								}}>
							Notifications
							</button>
						</li>
						<li>
							<button
								style={{
									background :'none',
									border : 'none',
									cursor : 'pointer',
									padding: 0
								}}
								onClick = {()=>{
									handleLogout();
									setHamburgerOpen(false);
								}}>
							Logout
							</button>
						</li>
					</ul>
				</div>
			)}
		</div>
	);
}

const styles = {
    container: {
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f5f5f0',
        padding: '20px'
    },
    card: {
        backgroundColor: 'white',
        padding: '40px',
        borderRadius: '8px',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        textAlign: 'center',
        border: '2px solid #5c4033',
        minWidth: '350px'
    },
    profilePictureContainer: {
        display: 'flex',
        justifyContent: 'center',
        marginBottom: '20px'
    },
    profilePicture: {
        width: '120px',
        height: '120px',
        borderRadius: '50%',
        objectFit: 'cover',
        border: '3px solid #5c4033'
    },
    profilePicturePlaceholder: {
        width: '120px',
        height: '120px',
        borderRadius: '50%',
        backgroundColor: '#5c4033',
        color: 'white',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '36px',
        fontWeight: 'bold'
    },
    title: {
        color: '#5c4033',
        marginBottom: '8px'
    },
    welcome: {
        fontSize: '22px',
        color: '#333',
        fontWeight: '600',
        marginBottom: '4px'
    },
    name: {
        fontSize: '16px',
        color: '#666',
        marginBottom: '8px'
    },
    info: {
        color: '#666',
        marginBottom: '20px'
    },
    infoCard: {
        backgroundColor: '#f9f9f9',
        borderRadius: '8px',
        padding: '16px',
        marginBottom: '24px',
        textAlign: 'left'
    },
    infoRow: {
        display: 'flex',
        justifyContent: 'space-between',
        padding: '8px 0',
        borderBottom: '1px solid #eee'
    },
    infoLabel: {
        color: '#666',
        fontWeight: '500'
    },
    infoValue: {
        color: '#333'
    },
    button: {
        padding: '12px 24px',
        backgroundColor: '#4a7c59',
        color: 'white',
        border: 'none',
        borderRadius: '4px',
        fontSize: '16px',
        cursor: 'pointer'
    }
};

export default SellerDashboard;