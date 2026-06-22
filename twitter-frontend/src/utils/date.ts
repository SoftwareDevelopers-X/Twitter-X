import { formatDistanceToNowStrict, parseISO, format } from 'date-fns';

export const formatRelativeTime = (dateString: string | undefined): string => {
  if (!dateString) return '';
  try {
    const date = typeof dateString === 'string' ? parseISO(dateString) : new Date(dateString);
    const relative = formatDistanceToNowStrict(date);
    
    // Convert e.g., "5 minutes" -> "5m", "2 hours" -> "2h", "1 day" -> "1d"
    return relative
      .replace(' seconds', 's')
      .replace(' second', 's')
      .replace(' minutes', 'm')
      .replace(' minute', 'm')
      .replace(' hours', 'h')
      .replace(' hour', 'h')
      .replace(' days', 'd')
      .replace(' day', 'd')
      .replace(' months', 'mo')
      .replace(' month', 'mo')
      .replace(' years', 'y')
      .replace(' year', 'y');
  } catch (error) {
    return '';
  }
};

export const formatJoinedDate = (dateString: string | undefined): string => {
  if (!dateString) return '';
  try {
    const date = typeof dateString === 'string' ? parseISO(dateString) : new Date(dateString);
    return format(date, 'MMMM yyyy');
  } catch (error) {
    return '';
  }
};

export const formatDateOfBirth = (dateString: string | undefined): string => {
  if (!dateString) return '';
  try {
    const date = typeof dateString === 'string' ? parseISO(dateString) : new Date(dateString);
    return format(date, 'MMMM d, yyyy');
  } catch (error) {
    return dateString;
  }
};
