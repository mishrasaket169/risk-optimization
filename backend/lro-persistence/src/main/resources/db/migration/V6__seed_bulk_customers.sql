-- V6: Seed bulk customer populations across all 4 bank schemas
-- bank_large: up to 10,000 customers (9,994 seeded + 6 existing)
-- bank_medium: up to 7,500 customers (7,496 seeded + 4 existing)
-- bank_small: up to 5,000 customers (4,996 seeded + 4 existing)
-- bank_distressed: up to 4,000 customers (3,996 seeded + 4 existing)
-- Segments ('Retail', 'SME', 'Corporate', 'Wealth') randomly distributed

-- =========================================================================
-- 1. bank_large (target: 10,000 customers)
-- =========================================================================
INSERT INTO bank_large.customers (customer_id, full_name, segment, kyc_status, location_category, risk_category, branch_name, created_at)
WITH gen AS (
    SELECT 
        i,
        (ARRAY['Retail', 'SME', 'Corporate', 'Wealth'])[1 + ((abs(hashtext(i::text || 'lrg_seg')) + floor(random() * 4)::int) % 4)] AS seg,
        (ARRAY['Tier 1', 'Tier 2', 'Tier 3', 'Rural'])[1 + ((abs(hashtext(i::text || 'lrg_loc')) + floor(random() * 4)::int) % 4)] AS loc
    FROM generate_series(7, 10000) AS s(i)
)
SELECT
    'CUST-LRG-' || (1000 + g.i) AS customer_id,
    CASE 
        WHEN g.seg IN ('Retail', 'Wealth') THEN
            (ARRAY['Aarav', 'Aditya', 'Arjun', 'Vivaan', 'Reyansh', 'Aayan', 'Krishna', 'Ishan', 'Shaurya', 'Atharv', 'Dhruv', 'Kabir', 'Rithvik', 'Ananya', 'Diya', 'Aadhya', 'Saanvi', 'Avani', 'Myra', 'Anvi', 'Anaya', 'Riya', 'Sneha', 'Pooja', 'Neha', 'Priya', 'Sunita', 'Rajesh', 'Suresh', 'Ramesh', 'Amit', 'Rahul', 'Vikas', 'Rohit', 'Manoj', 'Sanjay', 'Deepak', 'Sunil', 'Alok', 'Vijay', 'Ajay', 'Pankaj', 'Sandeep', 'Santosh', 'Prakash', 'Sachin', 'Dinesh', 'Kunal', 'Manish', 'Naveen'])[1 + (abs(hashtext(g.i::text || 'lrg_fn')) % 50)] || ' ' ||
            (ARRAY['Sharma', 'Verma', 'Patel', 'Mehta', 'Shah', 'Gupta', 'Singh', 'Kumar', 'Joshi', 'Kulkarni', 'Deshmukh', 'Shinde', 'Jadhav', 'Patil', 'Bhosale', 'More', 'Gaikwad', 'Pawar', 'Chavan', 'Sawant', 'Kadam', 'Naik', 'Salvi', 'Rao', 'Reddy', 'Nair', 'Menon', 'Iyer', 'Pillai', 'Banerjee', 'Chatterjee', 'Mukherjee', 'Ghosh', 'Das', 'Sen', 'Dutta', 'Bose', 'Agarwal', 'Bansal', 'Mittal', 'Singhal', 'Bhatia', 'Kapoor', 'Khanna', 'Malhotra', 'Soni', 'Seth', 'Chopra', 'Dewan', 'Pandey'])[1 + (abs(hashtext(g.i::text || 'lrg_ln')) % 50)]
        WHEN g.seg = 'Corporate' THEN
            (ARRAY['Apex', 'Bharat', 'Horizon', 'Deccan', 'Vidarbha', 'Sahyadri', 'Mahalakshmi', 'Zenith', 'Phoenix', 'Sterling', 'Prime', 'Global', 'National', 'Pinnacle', 'Vanguard', 'Omega', 'Delta', 'Paramount', 'Kalyan', 'Kaveri', 'Surya', 'Titanium', 'Godrej', 'Reliance', 'Tata', 'Shree', 'Omkar', 'Ganesh', 'Sai', 'Krishna'])[1 + (abs(hashtext(g.i::text || 'lrg_cp')) % 30)] || ' ' ||
            (ARRAY['Logistics', 'Synthetics', 'Automotives', 'Enterprises', 'Ventures', 'Infotech', 'Engineering', 'Chemicals', 'Textiles', 'Agro Products', 'Exports', 'Holdings', 'Industries', 'Solutions', 'Infrastructure', 'Metals', 'Polymers', 'Solar Tech', 'Minerals', 'Bio-Pharma'])[1 + (abs(hashtext(g.i::text || 'lrg_cm')) % 20)] || ' ' ||
            (ARRAY['Ltd', 'Pvt Ltd', 'LLP', 'Corp', 'Syndicate', 'Group', 'Partners', 'Enterprises', 'Holdings', 'Industries'])[1 + (abs(hashtext(g.i::text || 'lrg_cs')) % 10)]
        ELSE
            (ARRAY['Apex', 'Bharat', 'Horizon', 'Deccan', 'Vidarbha', 'Sahyadri', 'Mahalakshmi', 'Zenith', 'Phoenix', 'Sterling', 'Prime', 'Global', 'National', 'Pinnacle', 'Vanguard', 'Omega', 'Delta', 'Paramount', 'Kalyan', 'Kaveri', 'Surya', 'Titanium', 'Godrej', 'Reliance', 'Tata', 'Shree', 'Omkar', 'Ganesh', 'Sai', 'Krishna'])[1 + (abs(hashtext(g.i::text || 'lrg_sp')) % 30)] || ' ' ||
            (ARRAY['Traders', 'Auto Spares', 'Fabricators', 'Mills', 'Tools', 'Packaging', 'Components', 'Food Products', 'Hardware', 'Electricals', 'Precision Eng', 'Plastics', 'Textile Works', 'Forgings', 'Agro Services'])[1 + (abs(hashtext(g.i::text || 'lrg_sm')) % 15)] || ' ' ||
            (ARRAY['Pvt Ltd', 'LLP', 'Enterprises', 'Partners', 'Syndicate'])[1 + (abs(hashtext(g.i::text || 'lrg_ss')) % 5)]
    END AS full_name,
    g.seg AS segment,
    CASE WHEN random() < 0.90 THEN 'Verified' ELSE 'Not Verified' END AS kyc_status,
    g.loc AS location_category,
    CASE 
        WHEN random() < 0.65 THEN 'Low'
        WHEN random() < 0.90 THEN 'Medium'
        ELSE 'High'
    END AS risk_category,
    CASE g.loc
        WHEN 'Tier 1' THEN (ARRAY['Mumbai Fort', 'Mumbai BKC', 'Nariman Point', 'Delhi Connaught Place', 'Delhi Nehru Place', 'Bengaluru MG Road', 'Bengaluru Whitefield', 'Chennai Mount Road', 'Hyderabad Hitec City', 'Kolkata Park Street', 'Pune FC Road', 'Ahmedabad SG Highway'])[1 + (abs(hashtext(g.i::text || 'lrg_b1')) % 12)]
        WHEN 'Tier 2' THEN (ARRAY['Pune Shivaji Nagar', 'Ahmedabad Ashram Road', 'Jaipur MI Road', 'Lucknow Hazratganj', 'Surat Ring Road', 'Nagpur Civil Lines', 'Indore Vijay Nagar', 'Bhopal MP Nagar', 'Coimbatore RS Puram', 'Vadodara Alkapuri', 'Visakhapatnam Beach Road'])[1 + (abs(hashtext(g.i::text || 'lrg_b2')) % 11)]
        WHEN 'Tier 3' THEN (ARRAY['Kolhapur Town', 'Nashik City', 'Belagavi Fort Road', 'Udaipur City', 'Gwalior Main', 'Warangal Chowrasta', 'Salem Junction', 'Aligarh Center', 'Gorakhpur Town', 'Bhubaneswar Old Town'])[1 + (abs(hashtext(g.i::text || 'lrg_b3')) % 10)]
        ELSE (ARRAY['Karad Agro Branch', 'Sangli Market Branch', 'Satara Rural Branch', 'Baramati Krishi Branch', 'Anand Village Branch', 'Mehsana Rural Branch', 'Hoshiarpur Mandi', 'Warangal Rural Branch', 'Chhindwara Agri Center'])[1 + (abs(hashtext(g.i::text || 'lrg_b4')) % 9)]
    END AS branch_name,
    TIMESTAMPTZ '2024-01-01 09:00:00+05:30' + ((abs(hashtext(g.i::text || 'lrg_tm')) % 600) || ' days')::interval AS created_at
FROM gen g
ON CONFLICT (customer_id) DO NOTHING;

-- =========================================================================
-- 2. bank_medium (target: 7,500 customers)
-- =========================================================================
INSERT INTO bank_medium.customers (customer_id, full_name, segment, kyc_status, location_category, risk_category, branch_name, created_at)
WITH gen AS (
    SELECT 
        i,
        (ARRAY['Retail', 'SME', 'Corporate', 'Wealth'])[1 + ((abs(hashtext(i::text || 'med_seg')) + floor(random() * 4)::int) % 4)] AS seg,
        (ARRAY['Tier 1', 'Tier 2', 'Tier 3', 'Rural'])[1 + ((abs(hashtext(i::text || 'med_loc')) + floor(random() * 4)::int) % 4)] AS loc
    FROM generate_series(5, 7500) AS s(i)
)
SELECT
    'CUST-MED-' || (1000 + g.i) AS customer_id,
    CASE 
        WHEN g.seg IN ('Retail', 'Wealth') THEN
            (ARRAY['Aarav', 'Aditya', 'Arjun', 'Vivaan', 'Reyansh', 'Aayan', 'Krishna', 'Ishan', 'Shaurya', 'Atharv', 'Dhruv', 'Kabir', 'Rithvik', 'Ananya', 'Diya', 'Aadhya', 'Saanvi', 'Avani', 'Myra', 'Anvi', 'Anaya', 'Riya', 'Sneha', 'Pooja', 'Neha', 'Priya', 'Sunita', 'Rajesh', 'Suresh', 'Ramesh', 'Amit', 'Rahul', 'Vikas', 'Rohit', 'Manoj', 'Sanjay', 'Deepak', 'Sunil', 'Alok', 'Vijay', 'Ajay', 'Pankaj', 'Sandeep', 'Santosh', 'Prakash', 'Sachin', 'Dinesh', 'Kunal', 'Manish', 'Naveen'])[1 + (abs(hashtext(g.i::text || 'med_fn')) % 50)] || ' ' ||
            (ARRAY['Sharma', 'Verma', 'Patel', 'Mehta', 'Shah', 'Gupta', 'Singh', 'Kumar', 'Joshi', 'Kulkarni', 'Deshmukh', 'Shinde', 'Jadhav', 'Patil', 'Bhosale', 'More', 'Gaikwad', 'Pawar', 'Chavan', 'Sawant', 'Kadam', 'Naik', 'Salvi', 'Rao', 'Reddy', 'Nair', 'Menon', 'Iyer', 'Pillai', 'Banerjee', 'Chatterjee', 'Mukherjee', 'Ghosh', 'Das', 'Sen', 'Dutta', 'Bose', 'Agarwal', 'Bansal', 'Mittal', 'Singhal', 'Bhatia', 'Kapoor', 'Khanna', 'Malhotra', 'Soni', 'Seth', 'Chopra', 'Dewan', 'Pandey'])[1 + (abs(hashtext(g.i::text || 'med_ln')) % 50)]
        WHEN g.seg = 'Corporate' THEN
            (ARRAY['Apex', 'Bharat', 'Horizon', 'Deccan', 'Vidarbha', 'Sahyadri', 'Mahalakshmi', 'Zenith', 'Phoenix', 'Sterling', 'Prime', 'Global', 'National', 'Pinnacle', 'Vanguard', 'Omega', 'Delta', 'Paramount', 'Kalyan', 'Kaveri', 'Surya', 'Titanium', 'Godrej', 'Reliance', 'Tata', 'Shree', 'Omkar', 'Ganesh', 'Sai', 'Krishna'])[1 + (abs(hashtext(g.i::text || 'med_cp')) % 30)] || ' ' ||
            (ARRAY['Logistics', 'Synthetics', 'Automotives', 'Enterprises', 'Ventures', 'Infotech', 'Engineering', 'Chemicals', 'Textiles', 'Agro Products', 'Exports', 'Holdings', 'Industries', 'Solutions', 'Infrastructure', 'Metals', 'Polymers', 'Solar Tech', 'Minerals', 'Bio-Pharma'])[1 + (abs(hashtext(g.i::text || 'med_cm')) % 20)] || ' ' ||
            (ARRAY['Ltd', 'Pvt Ltd', 'LLP', 'Corp', 'Syndicate', 'Group', 'Partners', 'Enterprises', 'Holdings', 'Industries'])[1 + (abs(hashtext(g.i::text || 'med_cs')) % 10)]
        ELSE
            (ARRAY['Apex', 'Bharat', 'Horizon', 'Deccan', 'Vidarbha', 'Sahyadri', 'Mahalakshmi', 'Zenith', 'Phoenix', 'Sterling', 'Prime', 'Global', 'National', 'Pinnacle', 'Vanguard', 'Omega', 'Delta', 'Paramount', 'Kalyan', 'Kaveri', 'Surya', 'Titanium', 'Godrej', 'Reliance', 'Tata', 'Shree', 'Omkar', 'Ganesh', 'Sai', 'Krishna'])[1 + (abs(hashtext(g.i::text || 'med_sp')) % 30)] || ' ' ||
            (ARRAY['Traders', 'Auto Spares', 'Fabricators', 'Mills', 'Tools', 'Packaging', 'Components', 'Food Products', 'Hardware', 'Electricals', 'Precision Eng', 'Plastics', 'Textile Works', 'Forgings', 'Agro Services'])[1 + (abs(hashtext(g.i::text || 'med_sm')) % 15)] || ' ' ||
            (ARRAY['Pvt Ltd', 'LLP', 'Enterprises', 'Partners', 'Syndicate'])[1 + (abs(hashtext(g.i::text || 'med_ss')) % 5)]
    END AS full_name,
    g.seg AS segment,
    CASE WHEN random() < 0.88 THEN 'Verified' ELSE 'Not Verified' END AS kyc_status,
    g.loc AS location_category,
    CASE 
        WHEN random() < 0.50 THEN 'Low'
        WHEN random() < 0.85 THEN 'Medium'
        ELSE 'High'
    END AS risk_category,
    CASE g.loc
        WHEN 'Tier 1' THEN (ARRAY['Mumbai Fort', 'Mumbai BKC', 'Nariman Point', 'Delhi Connaught Place', 'Delhi Nehru Place', 'Bengaluru MG Road', 'Bengaluru Whitefield', 'Chennai Mount Road', 'Hyderabad Hitec City', 'Kolkata Park Street', 'Pune FC Road', 'Ahmedabad SG Highway'])[1 + (abs(hashtext(g.i::text || 'med_b1')) % 12)]
        WHEN 'Tier 2' THEN (ARRAY['Pune Shivaji Nagar', 'Ahmedabad Ashram Road', 'Jaipur MI Road', 'Lucknow Hazratganj', 'Surat Ring Road', 'Nagpur Civil Lines', 'Indore Vijay Nagar', 'Bhopal MP Nagar', 'Coimbatore RS Puram', 'Vadodara Alkapuri', 'Visakhapatnam Beach Road'])[1 + (abs(hashtext(g.i::text || 'med_b2')) % 11)]
        WHEN 'Tier 3' THEN (ARRAY['Kolhapur Town', 'Nashik City', 'Belagavi Fort Road', 'Udaipur City', 'Gwalior Main', 'Warangal Chowrasta', 'Salem Junction', 'Aligarh Center', 'Gorakhpur Town', 'Bhubaneswar Old Town'])[1 + (abs(hashtext(g.i::text || 'med_b3')) % 10)]
        ELSE (ARRAY['Karad Agro Branch', 'Sangli Market Branch', 'Satara Rural Branch', 'Baramati Krishi Branch', 'Anand Village Branch', 'Mehsana Rural Branch', 'Hoshiarpur Mandi', 'Warangal Rural Branch', 'Chhindwara Agri Center'])[1 + (abs(hashtext(g.i::text || 'med_b4')) % 9)]
    END AS branch_name,
    TIMESTAMPTZ '2024-01-01 09:00:00+05:30' + ((abs(hashtext(g.i::text || 'med_tm')) % 600) || ' days')::interval AS created_at
FROM gen g
ON CONFLICT (customer_id) DO NOTHING;

-- =========================================================================
-- 3. bank_small (target: 5,000 customers)
-- =========================================================================
INSERT INTO bank_small.customers (customer_id, full_name, segment, kyc_status, location_category, risk_category, branch_name, created_at)
WITH gen AS (
    SELECT 
        i,
        (ARRAY['Retail', 'SME', 'Corporate', 'Wealth'])[1 + ((abs(hashtext(i::text || 'sml_seg')) + floor(random() * 4)::int) % 4)] AS seg,
        (ARRAY['Tier 1', 'Tier 2', 'Tier 3', 'Rural'])[1 + ((abs(hashtext(i::text || 'sml_loc')) + floor(random() * 4)::int) % 4)] AS loc
    FROM generate_series(5, 5000) AS s(i)
)
SELECT
    'CUST-SML-' || (1000 + g.i) AS customer_id,
    CASE 
        WHEN g.seg IN ('Retail', 'Wealth') THEN
            (ARRAY['Aarav', 'Aditya', 'Arjun', 'Vivaan', 'Reyansh', 'Aayan', 'Krishna', 'Ishan', 'Shaurya', 'Atharv', 'Dhruv', 'Kabir', 'Rithvik', 'Ananya', 'Diya', 'Aadhya', 'Saanvi', 'Avani', 'Myra', 'Anvi', 'Anaya', 'Riya', 'Sneha', 'Pooja', 'Neha', 'Priya', 'Sunita', 'Rajesh', 'Suresh', 'Ramesh', 'Amit', 'Rahul', 'Vikas', 'Rohit', 'Manoj', 'Sanjay', 'Deepak', 'Sunil', 'Alok', 'Vijay', 'Ajay', 'Pankaj', 'Sandeep', 'Santosh', 'Prakash', 'Sachin', 'Dinesh', 'Kunal', 'Manish', 'Naveen'])[1 + (abs(hashtext(g.i::text || 'sml_fn')) % 50)] || ' ' ||
            (ARRAY['Sharma', 'Verma', 'Patel', 'Mehta', 'Shah', 'Gupta', 'Singh', 'Kumar', 'Joshi', 'Kulkarni', 'Deshmukh', 'Shinde', 'Jadhav', 'Patil', 'Bhosale', 'More', 'Gaikwad', 'Pawar', 'Chavan', 'Sawant', 'Kadam', 'Naik', 'Salvi', 'Rao', 'Reddy', 'Nair', 'Menon', 'Iyer', 'Pillai', 'Banerjee', 'Chatterjee', 'Mukherjee', 'Ghosh', 'Das', 'Sen', 'Dutta', 'Bose', 'Agarwal', 'Bansal', 'Mittal', 'Singhal', 'Bhatia', 'Kapoor', 'Khanna', 'Malhotra', 'Soni', 'Seth', 'Chopra', 'Dewan', 'Pandey'])[1 + (abs(hashtext(g.i::text || 'sml_ln')) % 50)]
        WHEN g.seg = 'Corporate' THEN
            (ARRAY['Apex', 'Bharat', 'Horizon', 'Deccan', 'Vidarbha', 'Sahyadri', 'Mahalakshmi', 'Zenith', 'Phoenix', 'Sterling', 'Prime', 'Global', 'National', 'Pinnacle', 'Vanguard', 'Omega', 'Delta', 'Paramount', 'Kalyan', 'Kaveri', 'Surya', 'Titanium', 'Godrej', 'Reliance', 'Tata', 'Shree', 'Omkar', 'Ganesh', 'Sai', 'Krishna'])[1 + (abs(hashtext(g.i::text || 'sml_cp')) % 30)] || ' ' ||
            (ARRAY['Logistics', 'Synthetics', 'Automotives', 'Enterprises', 'Ventures', 'Infotech', 'Engineering', 'Chemicals', 'Textiles', 'Agro Products', 'Exports', 'Holdings', 'Industries', 'Solutions', 'Infrastructure', 'Metals', 'Polymers', 'Solar Tech', 'Minerals', 'Bio-Pharma'])[1 + (abs(hashtext(g.i::text || 'sml_cm')) % 20)] || ' ' ||
            (ARRAY['Ltd', 'Pvt Ltd', 'LLP', 'Corp', 'Syndicate', 'Group', 'Partners', 'Enterprises', 'Holdings', 'Industries'])[1 + (abs(hashtext(g.i::text || 'sml_cs')) % 10)]
        ELSE
            (ARRAY['Apex', 'Bharat', 'Horizon', 'Deccan', 'Vidarbha', 'Sahyadri', 'Mahalakshmi', 'Zenith', 'Phoenix', 'Sterling', 'Prime', 'Global', 'National', 'Pinnacle', 'Vanguard', 'Omega', 'Delta', 'Paramount', 'Kalyan', 'Kaveri', 'Surya', 'Titanium', 'Godrej', 'Reliance', 'Tata', 'Shree', 'Omkar', 'Ganesh', 'Sai', 'Krishna'])[1 + (abs(hashtext(g.i::text || 'sml_sp')) % 30)] || ' ' ||
            (ARRAY['Traders', 'Auto Spares', 'Fabricators', 'Mills', 'Tools', 'Packaging', 'Components', 'Food Products', 'Hardware', 'Electricals', 'Precision Eng', 'Plastics', 'Textile Works', 'Forgings', 'Agro Services'])[1 + (abs(hashtext(g.i::text || 'sml_sm')) % 15)] || ' ' ||
            (ARRAY['Pvt Ltd', 'LLP', 'Enterprises', 'Partners', 'Syndicate'])[1 + (abs(hashtext(g.i::text || 'sml_ss')) % 5)]
    END AS full_name,
    g.seg AS segment,
    CASE WHEN random() < 0.85 THEN 'Verified' ELSE 'Not Verified' END AS kyc_status,
    g.loc AS location_category,
    CASE 
        WHEN random() < 0.40 THEN 'Low'
        WHEN random() < 0.80 THEN 'Medium'
        ELSE 'High'
    END AS risk_category,
    CASE g.loc
        WHEN 'Tier 1' THEN (ARRAY['Mumbai Fort', 'Mumbai BKC', 'Nariman Point', 'Delhi Connaught Place', 'Delhi Nehru Place', 'Bengaluru MG Road', 'Bengaluru Whitefield', 'Chennai Mount Road', 'Hyderabad Hitec City', 'Kolkata Park Street', 'Pune FC Road', 'Ahmedabad SG Highway'])[1 + (abs(hashtext(g.i::text || 'sml_b1')) % 12)]
        WHEN 'Tier 2' THEN (ARRAY['Pune Shivaji Nagar', 'Ahmedabad Ashram Road', 'Jaipur MI Road', 'Lucknow Hazratganj', 'Surat Ring Road', 'Nagpur Civil Lines', 'Indore Vijay Nagar', 'Bhopal MP Nagar', 'Coimbatore RS Puram', 'Vadodara Alkapuri', 'Visakhapatnam Beach Road'])[1 + (abs(hashtext(g.i::text || 'sml_b2')) % 11)]
        WHEN 'Tier 3' THEN (ARRAY['Kolhapur Town', 'Nashik City', 'Belagavi Fort Road', 'Udaipur City', 'Gwalior Main', 'Warangal Chowrasta', 'Salem Junction', 'Aligarh Center', 'Gorakhpur Town', 'Bhubaneswar Old Town'])[1 + (abs(hashtext(g.i::text || 'sml_b3')) % 10)]
        ELSE (ARRAY['Karad Agro Branch', 'Sangli Market Branch', 'Satara Rural Branch', 'Baramati Krishi Branch', 'Anand Village Branch', 'Mehsana Rural Branch', 'Hoshiarpur Mandi', 'Warangal Rural Branch', 'Chhindwara Agri Center'])[1 + (abs(hashtext(g.i::text || 'sml_b4')) % 9)]
    END AS branch_name,
    TIMESTAMPTZ '2024-01-01 09:00:00+05:30' + ((abs(hashtext(g.i::text || 'sml_tm')) % 600) || ' days')::interval AS created_at
FROM gen g
ON CONFLICT (customer_id) DO NOTHING;

-- =========================================================================
-- 4. bank_distressed (target: 4,000 customers)
-- =========================================================================
INSERT INTO bank_distressed.customers (customer_id, full_name, segment, kyc_status, location_category, risk_category, branch_name, created_at)
WITH gen AS (
    SELECT 
        i,
        (ARRAY['Retail', 'SME', 'Corporate', 'Wealth'])[1 + ((abs(hashtext(i::text || 'dst_seg')) + floor(random() * 4)::int) % 4)] AS seg,
        (ARRAY['Tier 1', 'Tier 2', 'Tier 3', 'Rural'])[1 + ((abs(hashtext(i::text || 'dst_loc')) + floor(random() * 4)::int) % 4)] AS loc
    FROM generate_series(5, 4000) AS s(i)
)
SELECT
    'CUST-DST-' || (1000 + g.i) AS customer_id,
    CASE 
        WHEN g.seg IN ('Retail', 'Wealth') THEN
            (ARRAY['Aarav', 'Aditya', 'Arjun', 'Vivaan', 'Reyansh', 'Aayan', 'Krishna', 'Ishan', 'Shaurya', 'Atharv', 'Dhruv', 'Kabir', 'Rithvik', 'Ananya', 'Diya', 'Aadhya', 'Saanvi', 'Avani', 'Myra', 'Anvi', 'Anaya', 'Riya', 'Sneha', 'Pooja', 'Neha', 'Priya', 'Sunita', 'Rajesh', 'Suresh', 'Ramesh', 'Amit', 'Rahul', 'Vikas', 'Rohit', 'Manoj', 'Sanjay', 'Deepak', 'Sunil', 'Alok', 'Vijay', 'Ajay', 'Pankaj', 'Sandeep', 'Santosh', 'Prakash', 'Sachin', 'Dinesh', 'Kunal', 'Manish', 'Naveen'])[1 + (abs(hashtext(g.i::text || 'dst_fn')) % 50)] || ' ' ||
            (ARRAY['Sharma', 'Verma', 'Patel', 'Mehta', 'Shah', 'Gupta', 'Singh', 'Kumar', 'Joshi', 'Kulkarni', 'Deshmukh', 'Shinde', 'Jadhav', 'Patil', 'Bhosale', 'More', 'Gaikwad', 'Pawar', 'Chavan', 'Sawant', 'Kadam', 'Naik', 'Salvi', 'Rao', 'Reddy', 'Nair', 'Menon', 'Iyer', 'Pillai', 'Banerjee', 'Chatterjee', 'Mukherjee', 'Ghosh', 'Das', 'Sen', 'Dutta', 'Bose', 'Agarwal', 'Bansal', 'Mittal', 'Singhal', 'Bhatia', 'Kapoor', 'Khanna', 'Malhotra', 'Soni', 'Seth', 'Chopra', 'Dewan', 'Pandey'])[1 + (abs(hashtext(g.i::text || 'dst_ln')) % 50)]
        WHEN g.seg = 'Corporate' THEN
            (ARRAY['Apex', 'Bharat', 'Horizon', 'Deccan', 'Vidarbha', 'Sahyadri', 'Mahalakshmi', 'Zenith', 'Phoenix', 'Sterling', 'Prime', 'Global', 'National', 'Pinnacle', 'Vanguard', 'Omega', 'Delta', 'Paramount', 'Kalyan', 'Kaveri', 'Surya', 'Titanium', 'Godrej', 'Reliance', 'Tata', 'Shree', 'Omkar', 'Ganesh', 'Sai', 'Krishna'])[1 + (abs(hashtext(g.i::text || 'dst_cp')) % 30)] || ' ' ||
            (ARRAY['Logistics', 'Synthetics', 'Automotives', 'Enterprises', 'Ventures', 'Infotech', 'Engineering', 'Chemicals', 'Textiles', 'Agro Products', 'Exports', 'Holdings', 'Industries', 'Solutions', 'Infrastructure', 'Metals', 'Polymers', 'Solar Tech', 'Minerals', 'Bio-Pharma'])[1 + (abs(hashtext(g.i::text || 'dst_cm')) % 20)] || ' ' ||
            (ARRAY['Ltd', 'Pvt Ltd', 'LLP', 'Corp', 'Syndicate', 'Group', 'Partners', 'Enterprises', 'Holdings', 'Industries'])[1 + (abs(hashtext(g.i::text || 'dst_cs')) % 10)]
        ELSE
            (ARRAY['Apex', 'Bharat', 'Horizon', 'Deccan', 'Vidarbha', 'Sahyadri', 'Mahalakshmi', 'Zenith', 'Phoenix', 'Sterling', 'Prime', 'Global', 'National', 'Pinnacle', 'Vanguard', 'Omega', 'Delta', 'Paramount', 'Kalyan', 'Kaveri', 'Surya', 'Titanium', 'Godrej', 'Reliance', 'Tata', 'Shree', 'Omkar', 'Ganesh', 'Sai', 'Krishna'])[1 + (abs(hashtext(g.i::text || 'dst_sp')) % 30)] || ' ' ||
            (ARRAY['Traders', 'Auto Spares', 'Fabricators', 'Mills', 'Tools', 'Packaging', 'Components', 'Food Products', 'Hardware', 'Electricals', 'Precision Eng', 'Plastics', 'Textile Works', 'Forgings', 'Agro Services'])[1 + (abs(hashtext(g.i::text || 'dst_sm')) % 15)] || ' ' ||
            (ARRAY['Pvt Ltd', 'LLP', 'Enterprises', 'Partners', 'Syndicate'])[1 + (abs(hashtext(g.i::text || 'dst_ss')) % 5)]
    END AS full_name,
    g.seg AS segment,
    CASE WHEN random() < 0.70 THEN 'Verified' ELSE 'Not Verified' END AS kyc_status,
    g.loc AS location_category,
    CASE 
        WHEN random() < 0.20 THEN 'Low'
        WHEN random() < 0.55 THEN 'Medium'
        ELSE 'High'
    END AS risk_category,
    CASE g.loc
        WHEN 'Tier 1' THEN (ARRAY['Mumbai Fort', 'Mumbai BKC', 'Nariman Point', 'Delhi Connaught Place', 'Delhi Nehru Place', 'Bengaluru MG Road', 'Bengaluru Whitefield', 'Chennai Mount Road', 'Hyderabad Hitec City', 'Kolkata Park Street', 'Pune FC Road', 'Ahmedabad SG Highway'])[1 + (abs(hashtext(g.i::text || 'dst_b1')) % 12)]
        WHEN 'Tier 2' THEN (ARRAY['Pune Shivaji Nagar', 'Ahmedabad Ashram Road', 'Jaipur MI Road', 'Lucknow Hazratganj', 'Surat Ring Road', 'Nagpur Civil Lines', 'Indore Vijay Nagar', 'Bhopal MP Nagar', 'Coimbatore RS Puram', 'Vadodara Alkapuri', 'Visakhapatnam Beach Road'])[1 + (abs(hashtext(g.i::text || 'dst_b2')) % 11)]
        WHEN 'Tier 3' THEN (ARRAY['Kolhapur Town', 'Nashik City', 'Belagavi Fort Road', 'Udaipur City', 'Gwalior Main', 'Warangal Chowrasta', 'Salem Junction', 'Aligarh Center', 'Gorakhpur Town', 'Bhubaneswar Old Town'])[1 + (abs(hashtext(g.i::text || 'dst_b3')) % 10)]
        ELSE (ARRAY['Karad Agro Branch', 'Sangli Market Branch', 'Satara Rural Branch', 'Baramati Krishi Branch', 'Anand Village Branch', 'Mehsana Rural Branch', 'Hoshiarpur Mandi', 'Warangal Rural Branch', 'Chhindwara Agri Center'])[1 + (abs(hashtext(g.i::text || 'dst_b4')) % 9)]
    END AS branch_name,
    TIMESTAMPTZ '2024-01-01 09:00:00+05:30' + ((abs(hashtext(g.i::text || 'dst_tm')) % 600) || ' days')::interval AS created_at
FROM gen g
ON CONFLICT (customer_id) DO NOTHING;
