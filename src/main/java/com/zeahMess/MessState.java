package com.zeahMess;

enum MessState
{
	IDLE,
	FILL_BOWLS,      // empty bowls → highlight sink
	GET_FLOUR,       // water bowls → highlight cupboard (flour)
	GET_TOMATO,      // pizza base → highlight cupboard (tomato)
	GET_CHEESE,      // incomplete pizza → highlight cupboard (cheese)
	GET_PINEAPPLE,   // uncooked pizza, no pineapple yet → highlight cupboard (pineapple)
	CUT_PINEAPPLE,   // whole pineapple in inv → highlight knife + pineapple in inventory
	COOK,            // pineapple chunks + uncooked pizza → highlight stove
	COMBINE_PIZZA    // pineapple chunks + plain pizza → highlight both in inventory
}
