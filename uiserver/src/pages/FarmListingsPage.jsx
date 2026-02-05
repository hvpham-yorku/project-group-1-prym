function FarmListingsPage(){
	return(
		<div>
			<h1 style={styles.header}>Farm Listings</h1>
			<div style={styles.container}>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>Farm Listing 1</button></a>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>Farm Listing 2</button></a>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>Farm Listing 3</button></a>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>Farm Listing 4</button></a>
				<a href='/farmlistingspage/farmlisting'><button style={styles.button}>Farm Listing 5</button></a>
			</div>
		</div>
	);
}



const styles = {
	container: {
		display: 'flex',
		flexDirection: 'column',
		alignItems: 'center',
	},
	button: {
		backgroundColor: '#04AA6D',
		color: 'white',
		fontSize: 100,
		padding: 12,
		width: 10000,
		border: 'none',
		borderRadius: 20,
		cursor: 'pointer',	
	},
	header: {
		backgroundColor: 'green',
	},
};

export default FarmListingsPage;