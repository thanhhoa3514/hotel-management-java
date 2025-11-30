-- Add capacity and size columns to room_types table
ALTER TABLE room_types
ADD COLUMN IF NOT EXISTS capacity INTEGER,
ADD COLUMN IF NOT EXISTS size DECIMAL(10,2);

-- Update existing room types with default values based on their type
UPDATE room_types
SET capacity = CASE
    WHEN LOWER(name) LIKE '%standard%' THEN 2
    WHEN LOWER(name) LIKE '%deluxe%' THEN 3
    WHEN LOWER(name) LIKE '%suite%' THEN 4
    WHEN LOWER(name) LIKE '%presidential%' THEN 6
    ELSE 2
END,
size = CASE
    WHEN LOWER(name) LIKE '%standard%' THEN 25.00
    WHEN LOWER(name) LIKE '%deluxe%' THEN 35.00
    WHEN LOWER(name) LIKE '%suite%' THEN 50.00
    WHEN LOWER(name) LIKE '%presidential%' THEN 80.00
    ELSE 25.00
END
WHERE capacity IS NULL OR size IS NULL;

-- Add comment to columns
COMMENT ON COLUMN room_types.capacity IS 'Maximum number of guests';
COMMENT ON COLUMN room_types.size IS 'Room size in square meters';

