function FarmListingsPage(){
	return(
		<div>
			<h1 style={styles.header}>Farm Listings</h1>
			<div style={styles.container}>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>Farm 1</button></a>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>Farm 2</button></a>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>Farm 3</button></a>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>Farm 4</button></a>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>Farm 5</button></a>
			</div>
		</div>
	);
}



const styles = {
	container: {
		display: 'flex',
		flexDirection: 'column',
		alignItems: 'center',
		backgroundColor: '#f5f5f0',
	},
	button: {
		backgroundColor: 'white',
		color: '#4a7c59',
		fontSize: 50,
		margin: 10,
		border: 'solid',
		borderColor: '#333',
		borderRadius: 8,
		width: 1200,  
		height: 200,
		cursor: 'pointer',	
	},
	header: {
		display: 'flex',
		alignItems: 'center',
		justifyContent: 'center',
		color: '#4a7c59',
		fontSize: 100,
	},
};

export default FarmListingsPage;