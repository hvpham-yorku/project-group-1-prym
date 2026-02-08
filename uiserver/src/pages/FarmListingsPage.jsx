const farms = [{
	id: 1, 
	name: 'farm1',
},
{
	id: 2, 
	name: 'farm2',	
},
{
	id: 3,
	name: 'farm3',
},
{
	id: 4,
	name: 'farm4',
},
{ 
	id: 5,
	name: 'farm5',
},
{
	id: 6,
	name: 'farm6',
}];


function FarmListingsPage(){
	const listItems = farms.map(farm => 
		<li key={farm.id}><a href='/farmlistingspage/farmlisting'><button style={styles.button}>{farm.name}</button></a></li>
	);
	
	return (
		<div>
			<h1 style={styles.header}>Farm Listings</h1>
			<div style={styles.container}>
				<ul>{listItems}</ul>
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
		fontFamily: 'Roboto',
		margin: 10,
		border: 'solid',
		borderColor: '#333',
		borderRadius: 8,
		width: 1300,
		height: 200,
		cursor: 'pointer',	
	},
	header: {
		fontFamily: 'Roboto',
		display: 'flex',
		alignItems: 'center',
		justifyContent: 'center',
		color: '#4a7c59',
		fontSize: 100,
	},
};

export default FarmListingsPage;