import { ReleaseInfo } from '../types';

export const parseHackerReleaseFile = (text: string): ReleaseInfo[] => {
  const releases: ReleaseInfo[] = [];
  
  // Split by brackets to isolate blocks
  // The format is [ ...content... ]
  // We split by ']' to get chunks, then clean up the '['
  const blocks = text.split(']');

  blocks.forEach(block => {
    // Remove opening bracket and trim whitespace
    const cleanBlock = block.replace('[', '').trim();
    
    if (!cleanBlock) return;

    // Split into lines
    const lines = cleanBlock.split('\n').map(line => line.trim()).filter(line => line.length > 0);

    // According to spec, the structure is roughly:
    // Line 1: Version
    // Line 2: Editions/Dates
    // Line 3+: News/Changelog
    
    if (lines.length >= 2) {
      const version = lines[0];
      const editions = lines[1];
      // Join the rest of the lines as the news description
      const news = lines.slice(2).join('\n');

      releases.push({
        version,
        editions,
        news
      });
    }
  });

  return releases;
};
