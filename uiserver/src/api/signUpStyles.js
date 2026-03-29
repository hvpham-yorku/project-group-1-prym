export function getStyling(buttonColour){
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
	        width: '100%',
	        maxWidth: '500px',
	        border: '2px solid #5c4033'
	    },
	    title: {
	        color: '#5c4033',
	        textAlign: 'center',
	        marginBottom: '8px',
	        fontSize: '28px'
	    },
	    subtitle: {
	        color: '#4a7c59',
	        textAlign: 'center',
	        marginBottom: '24px',
	        fontWeight: '500'
	    },
	    form: {
	        display: 'flex',
	        flexDirection: 'column',
	        gap: '16px'
	    },
	    row: {
	        display: 'flex',
	        gap: '16px'
	    },
	    halfWidth: {
	        flex: 1,
	        display: 'flex',
	        flexDirection: 'column',
	        gap: '4px'
	    },
	    inputGroup: {
	        display: 'flex',
	        flexDirection: 'column',
	        gap: '4px'
	    },
	    label: {
	        color: '#333',
	        fontWeight: '500'
	    },
	    input: {
	        padding: '12px',
	        borderRadius: '4px',
	        border: '1px solid #ccc',
	        fontSize: '16px'
	    },
	    button: {
	        padding: '12px',
	        backgroundColor: buttonColour,
	        color: 'white',
	        border: 'none',
	        borderRadius: '4px',
	        fontSize: '16px',
	        cursor: 'pointer',
	        marginTop: '8px'
	    },
	    error: {
	        backgroundColor: '#fee',
	        color: '#c00',
	        padding: '12px',
	        borderRadius: '4px',
	        marginBottom: '16px',
	        textAlign: 'center'
	    },
	    links: {
	        marginTop: '24px',
	        textAlign: 'center',
	        color: '#666'
	    },
	    link: {
	        color: '#4a7c59',
	        textDecoration: 'none',
	        fontWeight: '500'
	    },
	    imageUploadContainer: {
	        display: 'flex',
	        flexDirection: 'column',
	        alignItems: 'center',
	        gap: '12px'
	    },
	    imagePreview: {
	        width: '100px',
	        height: '100px',
	        borderRadius: '50%',
	        backgroundColor: '#e0e0e0',
	        display: 'flex',
	        alignItems: 'center',
	        justifyContent: 'center',
	        overflow: 'hidden',
	        border: '2px solid #ccc'
	    },
	    previewImg: {
	        width: '100%',
	        height: '100%',
	        objectFit: 'cover'
	    },
	    placeholderText: {
	        color: '#999',
	        fontSize: '12px'
	    },
	    uploadLabel: {
	        padding: '8px 16px',
	        backgroundColor: '#5c4033',
	        color: 'white',
	        borderRadius: '4px',
	        cursor: 'pointer',
	        fontSize: '14px'
	    },
	    fileInput: {
	        display: 'none'
	    },
	    hint: {
	        fontSize: '12px',
	        color: '#666',
	        marginTop: '4px'
	    }
	};
	return styles;
}

export default getStyling;